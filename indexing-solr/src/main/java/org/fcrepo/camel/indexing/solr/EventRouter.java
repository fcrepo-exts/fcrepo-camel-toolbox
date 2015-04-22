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
package org.fcrepo.camel.indexing.solr;

import static org.apache.camel.builder.PredicateBuilder.or;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_TRANSFORM;
import static org.fcrepo.camel.HttpMethods.POST;
import static org.fcrepo.camel.JmsHeaders.EVENT_TYPE;
import static org.fcrepo.camel.RdfNamespaces.INDEXING;
import static org.fcrepo.camel.RdfNamespaces.RDF;
import static org.fcrepo.camel.RdfNamespaces.REPOSITORY;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.builder.xml.XPathBuilder;

/**
 * A content router for handling JMS events.
 *
 * @author Aaron Coburn
 */
public class EventRouter extends RouteBuilder {

    private static final String logger = "org.fcrepo.camel.indexing";

    private static final String hasIndexingTransform = "/rdf:RDF/rdf:Description/indexing:hasIndexingTransform/text()";

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
            .routeId("FcrepoSolrRouter")
            .choice()
                .when(header(EVENT_TYPE).isEqualTo(REPOSITORY + "NODE_REMOVED"))
                    .to("direct:delete.solr")
                .otherwise()
                    .removeHeaders("CamelHttp*")
                    .to("fcrepo:{{fcrepo.baseUrl}}")
                    .setHeader(FCREPO_TRANSFORM).xpath(hasIndexingTransform, String.class, ns)
                    .removeHeaders("CamelHttp*")
                    .choice()
                        .when(or(simple("{{indexing.predicate}} != 'true'"), indexable))
                            .to("direct:update.solr")
                        .otherwise()
                            .to("direct:delete.solr");

        /**
         * Remove an item from the solr index.
         */
        from("direct:delete.solr")
            .routeId("FcrepoSolrDeleter")
            .process(new SolrDeleteProcessor())
            .log(LoggingLevel.INFO, logger,
                    "Deleting Solr Object ${headers[CamelFcrepoIdentifier]}")
            .setHeader(Exchange.HTTP_QUERY).simple("commitWithin={{solr.commitWithin}}")
            .to("http4://{{solr.baseUrl}}/update");

        /**
         * Perform the solr update.
         */
        from("direct:update.solr")
            .routeId("FcrepoSolrUpdater")
            .log(LoggingLevel.INFO, logger,
                    "Indexing Solr Object ${headers[CamelFcrepoIdentifier]} " +
                    "${headers[org.fcrepo.jms.identifier]}")
            .to("fcrepo:{{fcrepo.baseUrl}}?transform={{fcrepo.defaultTransform}}")
            .setHeader(Exchange.HTTP_METHOD).constant(POST)
            .setHeader(Exchange.HTTP_QUERY).simple("commitWithin={{solr.commitWithin}}")
            .to("http4://{{solr.baseUrl}}/update");
    }
}
