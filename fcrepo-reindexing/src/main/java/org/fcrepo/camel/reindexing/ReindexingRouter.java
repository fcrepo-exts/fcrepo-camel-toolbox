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

import org.apache.camel.ExchangePattern;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.fcrepo.camel.service.FcrepoCamelConfig;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import static java.net.InetAddress.getLocalHost;
import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.Exchange.HTTP_METHOD;
import static org.apache.camel.Exchange.HTTP_RESPONSE_CODE;
import static org.apache.camel.LoggingLevel.INFO;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_BASE_URL;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.fcrepo.camel.reindexing.ReindexingHeaders.REINDEXING_HOST;
import static org.fcrepo.camel.reindexing.ReindexingHeaders.REINDEXING_PORT;
import static org.fcrepo.camel.reindexing.ReindexingHeaders.REINDEXING_PREFIX;
import static org.fcrepo.camel.reindexing.ReindexingHeaders.REINDEXING_RECIPIENTS;
import static org.fcrepo.client.HttpMethods.GET;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A content router for handling JMS events.
 *
 * @author Aaron Coburn
 */
public class ReindexingRouter extends RouteBuilder {

    private static final Logger LOGGER = getLogger(ReindexingRouter.class);
    private static final int BAD_REQUEST = 400;
    private static final String LDP_CONTAINS = "<http://www.w3.org/ns/ldp#contains>";

    @Autowired
    private FcrepoReindexerConfig config;

    @Autowired
    private FcrepoCamelConfig fcrepoCamelConfig;

    /**
     * Configure the message route workflow.
     */
    public void configure() throws Exception {
        final String host = config.getRestHost();
        final String hostname = host.startsWith("http") ? host : "http://" + host;
        final int port = config.getRestPort();
        /**
         * A generic error handler (specific to this RouteBuilder)
         */
        onException(Exception.class)
                .maximumRedeliveries(config.getMaxRedeliveries())
            .log("Index Routing Error: ${routeId}");

        /**
         * Expose a RESTful endpoint for re-indexing
         */
        from("jetty:" + hostname + ":" + port + config.getRestPrefix() +
                "?matchOnUriPrefix=true&httpMethodRestrict=GET,POST")
                .routeId("FcrepoReindexingRest")
                .routeDescription("Expose the reindexing endpoint over HTTP")
                .setHeader(FCREPO_URI).simple("${headers.CamelHttpPath}")
            .choice()
                .when(header(HTTP_METHOD).isEqualTo("GET")).to("direct:usage")
                .otherwise().to("direct:reindex");

        from("direct:usage").routeId("FcrepoReindexingUsage")
                .setHeader(REINDEXING_PREFIX).simple(config.getRestPrefix())
                .setHeader(REINDEXING_PORT).simple(String.valueOf(port))
                .setHeader(FCREPO_BASE_URL).simple(fcrepoCamelConfig.getFcrepoBaseUrl())
            .process(exchange -> {
                exchange.getIn().setHeader(REINDEXING_HOST, getLocalHost().getHostName());
            })
            .to("mustache:org/fcrepo/camel/reindexing/usage.mustache");

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
                .to(ExchangePattern.InOnly, config.getReindexingStream() + "?disableTimeToLive=true")
                    .setHeader(CONTENT_TYPE).constant("text/plain")
                    .transform().simple("Indexing started at ${headers[CamelFcrepoUri]}");

        /**
         *  A route that traverses through a fedora heirarchy
         *  indexing nodes, as appropriate.
         */
        from(config.getReindexingStream() + "?asyncConsumer=true").routeId("FcrepoReindexingTraverse")
                .to(ExchangePattern.InOnly, "direct:recipients")
                .log(LoggingLevel.DEBUG, "Beginning traverse")
                .removeHeaders("CamelHttp*")
                .setHeader(HTTP_METHOD).constant(GET)
                .to("fcrepo:" + fcrepoCamelConfig.getFcrepoBaseUrl() + "?preferInclude=PreferContainment" +
                        "&preferOmit=ServerManaged&accept=application/n-triples")
            // split the n-triples stream on line breaks so that each triple is split into a separate message
            .split(body().tokenize("\\n")).streaming()
                .removeHeader(FCREPO_URI)
                .removeHeader("JMSCorrelationID")
                .process(exchange -> {
                    // This is a simple n-triples parser, spliting nodes on whitespace according to
                    // https://www.w3.org/TR/n-triples/#n-triples-grammar
                    // If the body is not null and the predicate is ldp:contains and the object is a URI,
                    // then set the CamelFcrepoUri header (if that header is not set, the processing stops
                    // at the filter() line below.
                    final String body = exchange.getIn().getBody(String.class);
                    if (body != null) {
                        final String parts[] = body.split("\\s+");
                        if (parts.length > 2 && parts[1].equals(LDP_CONTAINS) && parts[2].startsWith("<")) {
                            exchange.getIn().setHeader(FCREPO_URI, parts[2].substring(1, parts[2].length() - 1));
                        }
                        exchange.getIn().setBody(null);
                    }
                })
                .filter(header(FCREPO_URI).isNotNull())
                .to(ExchangePattern.InOnly, config.getReindexingStream() + "?disableTimeToLive=true");

        /**
         *  Send the message to all of the pre-determined endpoints
         */
        from("direct:recipients").routeId("FcrepoReindexingRecipients")
            .recipientList(header(REINDEXING_RECIPIENTS))
            .ignoreInvalidEndpoints();
    }
}
