/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel.indexing.solr;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.support.builder.Namespaces;
import org.apache.camel.builder.PredicateBuilder;
import org.apache.camel.Predicate;
import org.fcrepo.camel.processor.EventProcessor;
import org.fcrepo.camel.common.processor.AddBasicAuthProcessor;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.commons.lang3.StringUtils;
import static java.util.stream.Collectors.toList;
import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.Exchange.HTTP_METHOD;
import static org.apache.camel.Exchange.HTTP_QUERY;
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
    private FcrepoSolrIndexingConfig config;

    /**
     * Configure the message route workflow.
     */
    public void configure() throws Exception {

        logger.debug("Solr Router starting...");
        logger.trace("solr.indexing.predicate = '{}'", config.isIndexingPredicate());
        logger.trace("solr.checkHasIndexingTransformation = '{}'", config.isCheckHasIndexingTransformation());
        logger.trace("solr.defaultTransform = '{}'", config.getDefaultTransform());
        logger.trace("solr.input.stream = '{}'", config.getInputStream());
        logger.trace("solr.baseUrl = '{}'", config.getSolrBaseUrl());

        final Namespaces ns = new Namespaces("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        ns.add("indexing", "http://fedora.info/definitions/v4/indexing#");
        ns.add("ldp", "http://www.w3.org/ns/ldp#");

        final String solrUsername = config.getSolrUsername();
        final String solrPassword = config.getSolrPassword();
        final Predicate useSolrAuth = PredicateBuilder.constant(
                "true".equals(!StringUtils.isEmpty(solrUsername) && !StringUtils.isEmpty(solrPassword)));
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
<<<<<<< HEAD
                .when(and(not(constant(config.isIndexingPredicate())),
                          not(constant(config.isCheckHasIndexingTransformation()))))
                    .setHeader(INDEXING_TRANSFORMATION).simple(config.getDefaultTransform())
                    .log(LoggingLevel.TRACE, "Indexing Transformation set to: ${header.CamelIndexingTransformation}")
                    .log(LoggingLevel.INFO, "sending to update_solr")
                    .to("direct:update.solr")
                .otherwise()
                    .to(
                        "fcrepo:" + config.getFcrepoBaseUrl()
                        + "?preferOmit=PreferContainment&accept=application/rdf+xml"
                    )
                    .setHeader(INDEXING_TRANSFORMATION).xpath(hasIndexingTransformation, String.class, ns)
                    .log(LoggingLevel.TRACE, logger, "Indexing Transformation: ${header.CamelIndexingTransformation}")
                    .choice()
                        .when(or(header(INDEXING_TRANSFORMATION).isNull(),
                                 header(INDEXING_TRANSFORMATION).isEqualTo("")))
                            .setHeader(INDEXING_TRANSFORMATION).simple(config.getDefaultTransform())
                            .log(LoggingLevel.TRACE, logger, "No indexing transform found on the resource, using " +
                                    "default transform: ${header.CamelIndexingTransformation}")
                    .end()
                    .removeHeaders("CamelHttp*")
                    .choice()
                        .when(or(not(constant(config.isIndexingPredicate())),
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


        /*
         * Handle update operations
         */
        from("direct:update.solr").routeId("FcrepoSolrUpdater")
                .log(LoggingLevel.INFO, logger, "Indexing Solr Object ${header.CamelFcrepoUri}")
                .setHeader(INDEXING_URI).simple("${header.CamelFcrepoUri}")
                // Don't index the transformation itself
                .filter().simple("${header.CamelIndexingTransformation} != ${header.CamelIndexingUri}")
                .choice()
                    .when(and(header(INDEXING_TRANSFORMATION).isNotNull(),
                            header(INDEXING_TRANSFORMATION).isNotEqualTo("")))
                        .log(LoggingLevel.INFO, logger,
                            "Sending RDF for Transform with with XSLT from ${header.CamelIndexingTransformation}")
                        .toD("xslt:${header.CamelIndexingTransformation}")
                        .to("direct:send.to.solr")
                    .otherwise()
                        .log(LoggingLevel.INFO, logger, "Skipping ${header.CamelFcrepoUri}");


        /*
         * Send the transformed resource to Solr
         */
        from("direct:send.to.solr").routeId("FcrepoSolrSend")
                .log(LoggingLevel.INFO, logger, "sending to solr...")
                .removeHeaders("CamelHttp*")
                .setHeader(CONTENT_TYPE).constant("text/xml")
                .setHeader(HTTP_METHOD).constant("POST")
                .setHeader(HTTP_QUERY).simple("commitWithin=" + config.getCommitWithin())
                .choice()
                .when(useSolrAuth)
                    .process(new AddBasicAuthProcessor(solrUsername, solrPassword))
                    .log(LoggingLevel.DEBUG, logger, "Authenticating solr with: " + solrUsername + ":" + solrPassword)
                .otherwise()
                    .log(LoggingLevel.INFO, logger, "No Solr Auth provided")
                .to(config.getSolrBaseUrl() + "/update?useSystemProperties=true");

    }
}
