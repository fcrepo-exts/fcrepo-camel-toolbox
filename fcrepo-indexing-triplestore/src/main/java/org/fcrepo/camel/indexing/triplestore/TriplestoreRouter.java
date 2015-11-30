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
package org.fcrepo.camel.indexing.triplestore;

import static org.apache.camel.builder.PredicateBuilder.not;
import static org.apache.camel.builder.PredicateBuilder.or;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_NAMED_GRAPH;
import static org.fcrepo.camel.JmsHeaders.EVENT_TYPE;
import static org.fcrepo.camel.JmsHeaders.IDENTIFIER;
import static org.fcrepo.camel.RdfNamespaces.INDEXING;
import static org.fcrepo.camel.RdfNamespaces.RDF;
import static org.fcrepo.camel.RdfNamespaces.REPOSITORY;
import static org.slf4j.LoggerFactory.getLogger;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.builder.xml.XPathBuilder;
import org.fcrepo.camel.processor.SparqlDeleteProcessor;
import org.fcrepo.camel.processor.SparqlUpdateProcessor;
import org.slf4j.Logger;

/**
 * A content router for handling JMS events.
 *
 * @author Aaron Coburn
 */
public class TriplestoreRouter extends RouteBuilder {

    private static final Logger logger = getLogger(TriplestoreRouter.class);

    /**
     * Configure the message route workflow.
     */
    public void configure() throws Exception {

        final Namespaces ns = new Namespaces("rdf", RDF);
        ns.add("indexing", INDEXING);

        final XPathBuilder indexable = new XPathBuilder(
                String.format("/rdf:RDF/rdf:Description/rdf:type[@rdf:resource='%s']", INDEXING + "Indexable"));
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
            .choice()
                .when(header(EVENT_TYPE).isEqualTo(REPOSITORY + "NODE_REMOVED"))
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
            .filter(not(or(header(IDENTIFIER).startsWith(simple("{{audit.container}}/")),
                    header(IDENTIFIER).isEqualTo(simple("{{audit.container}}")))))
            .removeHeaders("CamelHttp*")
            .to("fcrepo:{{fcrepo.baseUrl}}?preferInclude=PreferMinimalContainer")
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
                    "Deleting Triplestore Object ${headers[CamelFcrepoIdentifier]} " +
                    "${headers[org.fcrepo.jms.identifier]}")
            .to("http4://{{triplestore.baseUrl}}");

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
                    "Indexing Triplestore Object ${headers[CamelFcrepoIdentifier]} " +
                    "${headers[org.fcrepo.jms.identifier]}")
            .to("http4://{{triplestore.baseUrl}}");
    }
}
