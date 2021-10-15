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

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.support.builder.Namespaces;
import org.fcrepo.camel.processor.EventProcessor;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import static java.util.stream.Collectors.toList;
import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.Exchange.HTTP_METHOD;
import static org.apache.camel.Exchange.HTTP_QUERY;
import static org.apache.camel.Exchange.HTTP_URI;
import static org.apache.camel.builder.PredicateBuilder.and;
import static org.apache.camel.builder.PredicateBuilder.in;
import static org.apache.camel.builder.PredicateBuilder.not;
import static org.apache.camel.builder.PredicateBuilder.or;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_EVENT_TYPE;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_RESOURCE_TYPE;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.fcrepo.camel.processor.ProcessorUtils.tokenizePropertyPlaceholder;
import static org.slf4j.LoggerFactory.getLogger;

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
    private static final String DELETE = "https://www.w3.org/ns/activitystreams#Delete";
    private static final String INDEXING_TRANSFORMATION = "CamelIndexingTransformation";
    private static final String INDEXABLE = "http://fedora.info/definitions/v4/indexing#Indexable";
    private static final String INDEXING_URI = "CamelIndexingUri";

    @Autowired
    private FcrepoSolrIndexerConfig config;

    /**
     * Configure the message route workflow.
     */
    public void configure() throws Exception {

        final Namespaces ns = new Namespaces("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        ns.add("indexing", "http://fedora.info/definitions/v4/indexing#");
        ns.add("ldp", "http://www.w3.org/ns/ldp#");


        /*
         * A generic error handler (specific to this RouteBuilder)
         */
        onException(Exception.class)
            .maximumRedeliveries(config.getMaxRedeliveries())
            .log("Index Routing Error: ${routeId}");

        /*
         * route a message to the proper queue, based on whether
         * it is a DELETE or UPDATE operation.
         */
        from(config.getInputStream())
            .routeId("FcrepoSolrRouter")
            .process(new EventProcessor())
            .choice()
                .when(or(header(FCREPO_EVENT_TYPE).contains(RESOURCE_DELETION),
                            header(FCREPO_EVENT_TYPE).contains(DELETE)))
                .log(LoggingLevel.TRACE, "Received message from Fedora routing to delete.solr")
                .to("direct:delete.solr")
                .otherwise()
                .log(LoggingLevel.TRACE, "Received message from Fedora routing to index.solr")
                .to("direct:index.solr");

        /*
         * Handle re-index events
         */
        from(config.getReindexStream())
            .routeId("FcrepoSolrReindex")
            .to("direct:index.solr");

        /*
         * Based on an item's metadata, determine if it is indexable.
         */
        from("direct:index.solr")
            .routeId("FcrepoSolrIndexer")
            .removeHeaders("CamelHttp*")
            .filter(not(in(tokenizePropertyPlaceholder(getContext(), config.getFilterContainers(), ",").stream()
                        .map(uri -> or(
                            header(FCREPO_URI).startsWith(constant(uri + "/")),
                            header(FCREPO_URI).isEqualTo(constant(uri))))
                        .collect(toList()))))
            .choice()
                .when(and(simple(config.isIndexingPredicate() + " != 'true'"),
                          simple(config.isCheckHasIndexingTransformation() + " != 'true'")))
                    .setHeader(INDEXING_TRANSFORMATION).simple(config.getDefaultTransform())
                    .to("direct:update.solr")
                .otherwise()
                    .to(
                        "fcrepo:" + config.getFcrepoBaseUrl()
                        + "?preferOmit=PreferContainment&accept=application/rdf+xml"
                    )
                    .setHeader(INDEXING_TRANSFORMATION).xpath(hasIndexingTransformation, String.class, ns)
                    .choice()
                        .when(or(header(INDEXING_TRANSFORMATION).isNull(),
                                 header(INDEXING_TRANSFORMATION).isEqualTo("")))
                            .setHeader(INDEXING_TRANSFORMATION).simple(config.getDefaultTransform()).end()
                    .removeHeaders("CamelHttp*")
                    .choice()
                        .when(or(simple(config.isIndexingPredicate() + " != 'true'"),
                                 header(FCREPO_RESOURCE_TYPE).contains(INDEXABLE)))
                            .to("direct:update.solr")
                        .otherwise()
                            .to("direct:delete.solr");


        /*
         * Remove an item from the solr index.
         */
        from("direct:delete.solr").routeId("FcrepoSolrDeleter")
                .removeHeaders("CamelHttp*")
                .to("mustache:org/fcrepo/camel/indexing/solr/delete.mustache")
                .log(LoggingLevel.INFO, logger, "Deleting Solr Object ${headers[CamelFcrepoUri]}")
                .setHeader(HTTP_METHOD).constant("POST")
                .setHeader(CONTENT_TYPE).constant("application/json")
                .setHeader(HTTP_QUERY).simple("commitWithin=" + config.getCommitWithin())
                .to(config.getSolrBaseUrl() + "/update?useSystemProperties=true");

        from("direct:external.ldpath").routeId("FcrepoSolrLdpathFetch")
                .removeHeaders("CamelHttp*")
                .setHeader(HTTP_URI).header(INDEXING_TRANSFORMATION)
                .setHeader(HTTP_METHOD).constant("GET")
                .to("http://localhost/ldpath");

        from("direct:transform.ldpath").routeId("FcrepoSolrTransform")
                .removeHeaders("CamelHttp*")
                .setHeader(HTTP_URI).simple(config.getLdpathServiceBaseUrl())
                .setHeader(HTTP_QUERY).simple("context=${headers.CamelFcrepoUri}")
                .to("http://localhost/ldpath");

        /*
         * Handle update operations
         */
        from("direct:update.solr").routeId("FcrepoSolrUpdater")
                .log(LoggingLevel.INFO, logger, "Indexing Solr Object ${header.CamelFcrepoUri}")
                .setBody(constant(null))
                .setHeader(INDEXING_URI).simple("${header.CamelFcrepoUri}")
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
                .log(LoggingLevel.INFO, logger, "Skipping ${header.CamelFcrepoUri}");

        /*
         * Send the transformed resource to Solr
         */
        from("direct:send.to.solr").routeId("FcrepoSolrSend")
                .log(LoggingLevel.INFO, logger, "sending to solr...")
                .removeHeaders("CamelHttp*")
                .setHeader(HTTP_METHOD).constant("POST")
                .setHeader(HTTP_QUERY).simple("commitWithin=" + config.getCommitWithin())
                .to(config.getSolrBaseUrl() + "/update?useSystemProperties=true");

    }
}
