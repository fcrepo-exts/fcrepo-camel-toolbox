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
package org.fcrepo.camel.indexing.solr;

import static org.apache.camel.Exchange.HTTP_METHOD;
import static org.apache.camel.Exchange.HTTP_QUERY;
import static org.apache.camel.Exchange.HTTP_URI;
import static org.apache.camel.builder.PredicateBuilder.not;
import static org.apache.camel.builder.PredicateBuilder.or;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_BASE_URL;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_IDENTIFIER;
import static org.fcrepo.camel.RdfNamespaces.INDEXING;
import static org.fcrepo.camel.RdfNamespaces.LDP;
import static org.fcrepo.camel.RdfNamespaces.RDF;
import static org.fcrepo.camel.RdfNamespaces.REPOSITORY;
import static org.slf4j.LoggerFactory.getLogger;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.builder.xml.XPathBuilder;
import org.slf4j.Logger;

/**
 * A content router for handling JMS events.
 *
 * @author Aaron Coburn
 */
public class SolrRouter extends RouteBuilder {

    private static final Logger logger = getLogger(SolrRouter.class);

    private static final String hasIndexingTransformation =
        "(/rdf:RDF/rdf:Description/indexing:hasIndexingTransformation/@rdf:resource | " +
        "/rdf:RDF/rdf:Description/indexing:hasIndexingTransformation/@rdf:about)[1]";

    private static final String RESOURCE_DELETION = "http://fedora.info/definitions/v4/event#ResourceDeletion";
    private static final String INDEXING_TRANSFORMATION = "CamelIndexingTransformation";
    private static final String INDEXING_URI = "CamelIndexingUri";

    /**
     * Configure the message route workflow.
     */
    public void configure() throws Exception {

        final Namespaces ns = new Namespaces("rdf", RDF);
        ns.add("indexing", INDEXING);
        ns.add("ldp", LDP);

        final XPathBuilder indexable = new XPathBuilder(
                String.format(
                    "/rdf:RDF/rdf:Description/rdf:type[@rdf:resource='%s']", INDEXING + "Indexable"));
        indexable.namespaces(ns);

        final XPathBuilder children = new XPathBuilder("/rdf:RDF/rdf:Description/ldp:contains");
        children.namespaces(ns);

        /*
         * A generic error handler (specific to this RouteBuilder)
         */
        onException(Exception.class)
            .maximumRedeliveries("{{error.maxRedeliveries}}")
            .log("Index Routing Error: ${routeId}");

        /*
         * route a message to the proper queue, based on whether
         * it is a DELETE or UPDATE operation.
         */
        from("{{input.stream}}")
            .routeId("FcrepoSolrRouter")
            .setHeader(FCREPO_IDENTIFIER).header("org.fcrepo.jms.identifier")
            .setHeader(FCREPO_BASE_URL).header("org.fcrepo.jms.baseUrl")
            .choice()
                // this clause supports Fedora 4.5.1 and earlier but may be removed in a future release
                .when(header("org.fcrepo.jms.eventType").isEqualTo(REPOSITORY + "NODE_REMOVED"))
                    .to("direct:delete.solr")
                .when(header("org.fcrepo.jms.eventType").isEqualTo(RESOURCE_DELETION))
                    .to("direct:delete.solr")
                .otherwise()
                    .to("direct:index.solr");

        /*
         * Handle re-index events
         */
        from("{{solr.reindex.stream}}")
            .routeId("FcrepoSolrReindex")
            .to("direct:index.solr");

        /*
         * Based on an item's metadata, determine if it is indexable.
         */
        from("direct:index.solr")
            .routeId("FcrepoSolrIndexer")
            .removeHeaders("CamelHttp*")
            .filter(not(or(header(FCREPO_IDENTIFIER).startsWith(simple("{{audit.container}}/")),
                    header(FCREPO_IDENTIFIER).isEqualTo(simple("{{audit.container}}")))))
            .to("fcrepo:{{fcrepo.baseUrl}}?preferOmit=PreferContainment&accept=application/rdf+xml")
            .setHeader(INDEXING_TRANSFORMATION).xpath(hasIndexingTransformation, String.class, ns)
            .choice()
                .when(or(header(INDEXING_TRANSFORMATION).isNull(), header(INDEXING_TRANSFORMATION).isEqualTo("")))
                    .setHeader(INDEXING_TRANSFORMATION).simple("{{fcrepo.defaultTransform}}").end()
            .removeHeaders("CamelHttp*")
            .choice()
                .when(or(simple("{{indexing.predicate}} != 'true'"), indexable))
                    .to("direct:update.solr")
                .otherwise()
                    .to("direct:delete.solr");

        /*
         * Remove an item from the solr index.
         */
        from("direct:delete.solr").routeId("FcrepoSolrDeleter")
            .removeHeaders("CamelHttp*")
            .process(new SolrDeleteProcessor())
            .log(LoggingLevel.INFO, logger, "Deleting Solr Object ${headers[CamelFcrepoIdentifier]}")
            .setHeader(HTTP_QUERY).simple("commitWithin={{solr.commitWithin}}")
            .to("{{solr.baseUrl}}/update?useSystemProperties=true");

        from("direct:external.ldpath").routeId("FcrepoSolrLdpathFetch")
            .removeHeaders("CamelHttp*")
            .setHeader(HTTP_URI).header(INDEXING_TRANSFORMATION)
            .setHeader(HTTP_METHOD).constant("GET")
            .to("http4://localhost/ldpath");

        from("direct:transform.ldpath").routeId("FcrepoSolrTransform")
            .removeHeaders("CamelHttp*")
            .setHeader(HTTP_URI).simple("{{ldpath.service.baseUrl}}${header.CamelFcrepoIdentifier}")
            .to("http4://localhost/ldpath");

        /*
         * Handle update operations
         */
        from("direct:update.solr").routeId("FcrepoSolrUpdater")
            .log(LoggingLevel.INFO, logger, "Indexing Solr Object ${header.CamelFcrepoIdentifier}")
            .setBody(constant(null))
            .setHeader(INDEXING_URI).simple("${header.CamelFcrepoBaseUrl}${header.CamelFcrepoIdentifier}")
            // Don't index the transformation itself
            .filter().simple("${header.CamelIndexingTransformation} != ${header.CamelIndexingUri}")
                .choice()
                    .when(header(INDEXING_TRANSFORMATION).startsWith("http"))
                        .log(LoggingLevel.INFO, logger,
                                "Fetching external LDPath program from ${header.CamelIndexingTransformation}")
                        .to("direct:external.ldpath")
                        .setHeader(HTTP_METHOD).constant("POST")
                        .to("direct:transform.ldpath")
                        .to("direct:send.to.solr")
                    .when(or(header(INDEXING_TRANSFORMATION).isNull(), header(INDEXING_TRANSFORMATION).isEqualTo("")))
                        .setHeader(HTTP_METHOD).constant("GET")
                        .to("direct:transform.ldpath")
                        .to("direct:send.to.solr")
                    .otherwise()
                        .log(LoggingLevel.INFO, logger, "Skipping ${header.CamelFcrepoIdentifier}");

        /*
         * Send the transformed resource to Solr
         */
        from("direct:send.to.solr").routeId("FcrepoSolrSend")
            .removeHeaders("CamelHttp*")
            .setHeader(HTTP_METHOD).constant("POST")
            .setHeader(HTTP_QUERY).simple("commitWithin={{solr.commitWithin}}")
            .to("{{solr.baseUrl}}/update?useSystemProperties=true");
    }
}
