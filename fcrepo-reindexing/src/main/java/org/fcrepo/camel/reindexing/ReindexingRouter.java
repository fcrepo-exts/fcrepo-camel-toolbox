/**
 * Copyright 2015 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import static org.fcrepo.camel.FcrepoHeaders.FCREPO_BASE_URL;
import static org.slf4j.LoggerFactory.getLogger;

import javax.xml.transform.stream.StreamSource;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.builder.xml.XPathBuilder;
import org.fcrepo.camel.HttpMethods;
import org.fcrepo.camel.RdfNamespaces;
import org.slf4j.Logger;

/**
 * A content router for handling JMS events.
 *
 * @author Aaron Coburn
 */
public class ReindexingRouter extends RouteBuilder {

    private static final Logger logger = getLogger(ReindexingRouter.class);

    /**
     * Configure the message route workflow.
     */
    public void configure() throws Exception {

        final Namespaces ns = new Namespaces("rdf", RdfNamespaces.RDF);
        ns.add("ldp", RdfNamespaces.LDP);

        final XPathBuilder children = new XPathBuilder("/rdf:RDF/rdf:Description/ldp:contains");
        children.namespaces(ns);

        /**
         * A generic error handler (specific to this RouteBuilder)
         */
        onException(Exception.class)
            .maximumRedeliveries("{{error.maxRedeliveries}}")
            .log("Index Routing Error: ${routeId}");

        /**
         * Expose a RESTful endpoint for re-indexing
         */
        rest("{{rest.prefix}}")
            .get().to("direct:usage")
            .post().consumes("application/json").to("direct:reindex");

        from("direct:usage")
            .routeId("FcrepoReindexingUsage")
            .setHeader(ReindexingHeaders.REST_PREFIX).simple("{{rest.prefix}}")
            .setHeader(ReindexingHeaders.REST_PORT).simple("{{rest.port}}")
            .setHeader(FCREPO_BASE_URL).simple("{{fcrepo.baseUrl}}")
            .process(new UsageProcessor());

        /**
         * A Re-indexing endpoint, setting where in the fcrepo hierarchy
         * a re-indexing operation should begin.
         */
        from("direct:reindex")
            .routeId("FcrepoReindexingReindex")
            .setHeader(ReindexingHeaders.REST_PREFIX).simple("{{rest.prefix}}")
            .setHeader(FCREPO_BASE_URL).simple("{{fcrepo.baseUrl}}")
            .process(new RestProcessor())
            .choice()
                .when(header(Exchange.HTTP_RESPONSE_CODE).isGreaterThanOrEqualTo(400))
                    .endChoice()
                .when(header(ReindexingHeaders.RECIPIENTS).isEqualTo(""))
                    .transform().simple("No endpoints configured for indexing")
                    .endChoice()
                .otherwise()
                    .log(LoggingLevel.INFO, logger, "Initial indexing path: ${headers[CamelFcrepoIdentifier]}")
                    .inOnly("{{reindexing.stream}}?disableTimeToLive=true")
                    .setHeader(Exchange.CONTENT_TYPE).constant("text/plain")
                    .transform().simple("Indexing started at ${headers[CamelFcrepoIdentifier]}");

        /**
         *  A route that traverses through a fedora heirarchy
         *  indexing nodes, as appropriate.
         */
        from("{{reindexing.stream}}?asyncConsumer=true")
            .routeId("FcrepoReindexingTraverse")
            .streamCaching()
            .inOnly("direct:recipients")
            .removeHeaders("CamelHttp*")
            .setHeader(Exchange.HTTP_METHOD).constant(HttpMethods.GET)
            .to("fcrepo:{{fcrepo.baseUrl}}?preferInclude=PreferContainment&preferOmit=ServerManaged")
            .convertBodyTo(StreamSource.class)
            .split(children).streaming()
                .transform().xpath("/ldp:contains/@rdf:resource", String.class, ns)
                .process(new PathProcessor())
                .inOnly("{{reindexing.stream}}?disableTimeToLive=true");

        /**
         *  Send the message to all of the pre-determined endpoints
         */
        from("direct:recipients")
            .routeId("FcrepoReindexingRecipients")
            .recipientList(header(ReindexingHeaders.RECIPIENTS))
            .ignoreInvalidEndpoints();
    }
}
