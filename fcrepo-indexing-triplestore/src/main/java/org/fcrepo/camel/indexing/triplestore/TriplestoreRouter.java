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
package org.fcrepo.camel.indexing.triplestore;

import static org.apache.camel.builder.PredicateBuilder.not;
import static org.apache.camel.builder.PredicateBuilder.or;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_NAMED_GRAPH;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_EVENT_TYPE;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.slf4j.LoggerFactory.getLogger;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.builder.xml.XPathBuilder;
import org.fcrepo.camel.processor.EventProcessor;
import org.fcrepo.camel.processor.SparqlDeleteProcessor;
import org.fcrepo.camel.processor.SparqlUpdateProcessor;
import org.slf4j.Logger;

/**
 * A content router for handling Fedora events.
 *
 * @author Aaron Coburn
 */
public class TriplestoreRouter extends RouteBuilder {

    private static final Logger logger = getLogger(TriplestoreRouter.class);

    private static final String RESOURCE_DELETION = "http://fedora.info/definitions/v4/event#ResourceDeletion";
    private static final String DELETE = "https://www.w3.org/ns/activitystreams#Delete";

    /**
     * Configure the message route workflow.
     */
    public void configure() throws Exception {

        final Namespaces ns = new Namespaces("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        ns.add("indexing", "http://fedora.info/definitions/v4/indexing#");

        final XPathBuilder indexable = new XPathBuilder(
                String.format("/rdf:RDF/rdf:Description/rdf:type[@rdf:resource='%s']",
                    "http://fedora.info/definitions/v4/indexing#Indexable"));
        indexable.namespaces(ns);

        /**
         * A generic error handler (specific to this RouteBuilder)
         */
        onException(Exception.class)
            .maximumRedeliveries("{{error.maxRedeliveries}}")
            .log("Index Routing Error: ${routeId}");

        /**
         * route a message to the proper queue, based on whether
         * it is a DELETE or UPDATE operation.
         */
        from("{{input.stream}}")
            .routeId("FcrepoTriplestoreRouter")
            .process(new EventProcessor())
            .choice()
                .when(or(header(FCREPO_EVENT_TYPE).contains(RESOURCE_DELETION),
                            header(FCREPO_EVENT_TYPE).contains(DELETE)))
                    .to("direct:delete.triplestore")
                .otherwise()
                    .to("direct:index.triplestore");

        /**
         * Handle re-index events
         */
        from("{{triplestore.reindex.stream}}")
            .routeId("FcrepoTriplestoreReindex")
            .to("direct:index.triplestore");

        /**
         * Based on an item's metadata, determine if it is indexable.
         */
        from("direct:index.triplestore")
            .routeId("FcrepoTriplestoreIndexer")
            .filter(not(or(header(FCREPO_URI).startsWith(simple("{{audit.container}}/")),
                    header(FCREPO_URI).isEqualTo(simple("{{audit.container}}")))))
            .removeHeaders("CamelHttp*")
            .to("fcrepo:{{fcrepo.baseUrl}}?preferInclude=PreferMinimalContainer&accept=application/rdf+xml")
            .choice()
                .when(or(simple("{{indexing.predicate}} != 'true'"), indexable))
                    .to("direct:update.triplestore")
                .otherwise()
                    .to("direct:delete.triplestore");

        /**
         * Remove an item from the triplestore index.
         */
        from("direct:delete.triplestore")
            .routeId("FcrepoTriplestoreDeleter")
            .process(new SparqlDeleteProcessor())
            .log(LoggingLevel.INFO, logger,
                    "Deleting Triplestore Object ${headers[CamelFcrepoUri]}")
            .to("{{triplestore.baseUrl}}?useSystemProperties=true");

        /**
         * Perform the sparql update.
         */
        from("direct:update.triplestore")
            .routeId("FcrepoTriplestoreUpdater")
            .setHeader(FCREPO_NAMED_GRAPH)
                .simple("{{triplestore.namedGraph}}")
            .to("fcrepo:{{fcrepo.baseUrl}}?accept=application/n-triples" +
                    "&preferOmit={{prefer.omit}}&preferInclude={{prefer.include}}")
            .process(new SparqlUpdateProcessor())
            .log(LoggingLevel.INFO, logger,
                    "Indexing Triplestore Object ${headers[CamelFcrepoUri]}")
            .to("{{triplestore.baseUrl}}?useSystemProperties=true");
    }
}
