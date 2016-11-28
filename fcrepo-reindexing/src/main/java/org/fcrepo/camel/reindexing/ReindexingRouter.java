/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.camel.reindexing;

import static java.net.InetAddress.getLocalHost;
import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.Exchange.HTTP_METHOD;
import static org.apache.camel.Exchange.HTTP_RESPONSE_CODE;
import static org.apache.camel.LoggingLevel.INFO;
import static org.fcrepo.camel.reindexing.ReindexingHeaders.REINDEXING_RECIPIENTS;
import static org.fcrepo.camel.reindexing.ReindexingHeaders.REINDEXING_HOST;
import static org.fcrepo.camel.reindexing.ReindexingHeaders.REINDEXING_PORT;
import static org.fcrepo.camel.reindexing.ReindexingHeaders.REINDEXING_PREFIX;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_BASE_URL;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.fcrepo.client.HttpMethods.GET;
import static org.slf4j.LoggerFactory.getLogger;

import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;

/**
 * A content router for handling JMS events.
 *
 * @author Aaron Coburn
 */
public class ReindexingRouter extends RouteBuilder {

    private static final Logger LOGGER = getLogger(ReindexingRouter.class);
    private static final int BAD_REQUEST = 400;

    @PropertyInject(value = "rest.port", defaultValue = "9080")
    private String port;

    @PropertyInject(value = "rest.host", defaultValue = "localhost")
    private String host;

    /**
     * Configure the message route workflow.
     */
    public void configure() throws Exception {

        final String hostname = host.startsWith("http") ? host : "http://" + host;

        /**
         * A generic error handler (specific to this RouteBuilder)
         */
        onException(Exception.class)
            .maximumRedeliveries("{{error.maxRedeliveries}}")
            .log("Index Routing Error: ${routeId}");

        /**
         * Expose a RESTful endpoint for re-indexing
         */
        from("jetty:" + hostname + ":" + port + "{{rest.prefix}}?matchOnUriPrefix=true&httpMethodRestrict=GET,POST")
            .routeId("FcrepoReindexingRest")
            .routeDescription("Expose the reindexing endpoint over HTTP")
            .setHeader(FCREPO_URI).simple("{{fcrepo.baseUrl}}${headers.CamelHttpPath}")
            .choice()
                .when(header(HTTP_METHOD).isEqualTo("GET")).to("direct:usage")
                .otherwise().to("direct:reindex");

        from("direct:usage").routeId("FcrepoReindexingUsage")
            .setHeader(REINDEXING_PREFIX).simple("{{rest.prefix}}")
            .setHeader(REINDEXING_PORT).simple(port)
            .setHeader(FCREPO_BASE_URL).simple("{{fcrepo.baseUrl}}")
            .process(exchange -> {
                exchange.getIn().setHeader(REINDEXING_HOST, getLocalHost().getHostName());
            })
            .to("mustache:usage.mustache");

        /**
         * A Re-indexing endpoint, setting where in the fcrepo hierarchy
         * a re-indexing operation should begin.
         */
        from("direct:reindex").routeId("FcrepoReindexingReindex")
            .process(new RestProcessor())
            .removeHeaders("CamelHttp*")
            .removeHeader("JMSCorrelationID")
            .setBody(constant(null))
            .choice()
                .when(header(HTTP_RESPONSE_CODE).isGreaterThanOrEqualTo(BAD_REQUEST))
                    .endChoice()
                .when(header(REINDEXING_RECIPIENTS).isEqualTo(""))
                    .transform().simple("No endpoints configured for indexing")
                    .endChoice()
                .otherwise()
                    .log(INFO, LOGGER, "Initial indexing path: ${headers[CamelFcrepoUri]}")
                    .inOnly("{{reindexing.stream}}?disableTimeToLive=true")
                    .setHeader(CONTENT_TYPE).constant("text/plain")
                    .transform().simple("Indexing started at ${headers[CamelFcrepoUri]}");

        /**
         *  A route that traverses through a fedora heirarchy
         *  indexing nodes, as appropriate.
         */
        from("{{reindexing.stream}}?asyncConsumer=true").routeId("FcrepoReindexingTraverse")
            .inOnly("direct:recipients")
            .removeHeaders("CamelHttp*")
            .setHeader(HTTP_METHOD).constant(GET)
            .to("fcrepo:{{fcrepo.baseUrl}}?preferInclude=PreferContainment" +
                    "&preferOmit=ServerManaged&accept=application/n-triples")
            .split(body().tokenize("\\n")).streaming()
                .filter(body().contains("<http://www.w3.org/ns/ldp#contains>"))
                    .removeHeader(FCREPO_URI)
                    .removeHeader("JMSCorrelationID")
                    .process(exchange -> {
                        final String body = exchange.getIn().getBody(String.class);
                        if (body != null) {
                            final String parts[] = body.split("\\s+");
                            if (parts.length > 2 && parts[2].startsWith("<")) {
                                exchange.getIn().setHeader(FCREPO_URI, parts[2].substring(1, parts[2].length() - 1));
                            }
                            exchange.getIn().setBody(null);
                        }
                    })
                    .filter(header(FCREPO_URI).isNotNull())
                        .inOnly("{{reindexing.stream}}?disableTimeToLive=true");

        /**
         *  Send the message to all of the pre-determined endpoints
         */
        from("direct:recipients").routeId("FcrepoReindexingRecipients")
            .recipientList(header(REINDEXING_RECIPIENTS))
            .ignoreInvalidEndpoints();
    }
}
