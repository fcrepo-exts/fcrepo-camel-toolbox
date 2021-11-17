/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel.httpforwarding;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.fcrepo.camel.common.processor.AddBasicAuthProcessor;
import org.apache.camel.support.builder.Namespaces;
import org.fcrepo.camel.processor.EventProcessor;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import static org.apache.camel.builder.PredicateBuilder.in;
import static org.apache.camel.builder.PredicateBuilder.not;
import static org.apache.camel.builder.PredicateBuilder.or;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static java.util.stream.Collectors.toList;
import static org.fcrepo.camel.processor.ProcessorUtils.tokenizePropertyPlaceholder;

import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.Exchange.HTTP_METHOD;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_EVENT_TYPE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A content router for handling JMS events.
 *
 * @author Geoff Scholl
 * @author Demian Katz
 */
public class HttpRouter extends RouteBuilder {

    private static final Logger LOGGER = getLogger(HttpRouter.class);

    @Autowired
    private FcrepoHttpForwardingConfig config;

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
         * route a message to the proper queue
         */
        from(config.getInputStream())
            .routeId("FcrepoHttpRouter")
            .process(new EventProcessor())
            .log(LoggingLevel.TRACE, "Received message from Fedora routing to index.http")
            .to("direct:add.type.to.http.message");

        /*
         * Handle re-index events
         */
        from(config.getReindexStream())
            .routeId("FcrepoHttpReindex")
            .to("direct:add.type.to.http.message");

        /*
         * Add event type header to message; we want to use the header.org.fcrepo.jms.eventtype
         * value when it is available. If it is unset, that likely indicates a reindex operation,
         * in which case we should default to an Update.
         */
        from("direct:add.type.to.http.message")
            .routeId("FcrepoHttpAddType")
            .choice()
            .when(simple("${header.org.fcrepo.jms.eventtype}"))
                .setHeader(FCREPO_EVENT_TYPE).simple("${header.org.fcrepo.jms.eventtype}")
                .to("direct:send.to.http")
            .otherwise()
                .setHeader(FCREPO_EVENT_TYPE).constant("https://www.w3.org/ns/activitystreams#Update")
                .to("direct:send.to.http");

        /*
         * Forward message to Http
         */
        from("direct:send.to.http").routeId("FcrepoHttpSend")
            .filter(not(in(tokenizePropertyPlaceholder(getContext(), config.getFilterContainers(), ",").stream()
                .map(uri -> or(
                    header(FCREPO_URI).startsWith(constant(uri + "/")),
                    header(FCREPO_URI).isEqualTo(constant(uri))))
                .collect(toList()))))
            .log(LoggingLevel.INFO, LOGGER, "sending ${headers[CamelFcrepoUri]} to http endpoint...")
            .to("mustache:org/fcrepo/camel/httpforwarding/httpMessage.mustache")
            .setHeader(HTTP_METHOD).constant("POST")
            .setHeader(CONTENT_TYPE).constant("application/json")
            .process(new AddBasicAuthProcessor(config.getHttpAuthUsername(), config.getHttpAuthPassword()))
            .to(config.getHttpBaseUrl().isEmpty() ? "direct:http.baseurl.missing" : config.getHttpBaseUrl());

        /*
         * Stop the route if configuration is incomplete.
         */
        from("direct:http.baseurl.missing")
            .routeId("FcrepoHttpBaseUrlMissing")
            .log(LoggingLevel.ERROR, LOGGER, "Cannot forward HTTP message because http.baseUrl property is empty.")
            .stop();
    }
}
