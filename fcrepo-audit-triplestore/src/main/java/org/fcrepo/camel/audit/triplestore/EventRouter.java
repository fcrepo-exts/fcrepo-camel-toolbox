/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel.audit.triplestore;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.fcrepo.camel.common.processor.AddBasicAuthProcessor;
import org.fcrepo.camel.processor.EventProcessor;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import static java.util.stream.Collectors.toList;
import static org.apache.camel.builder.PredicateBuilder.in;
import static org.apache.camel.builder.PredicateBuilder.not;
import static org.apache.camel.builder.PredicateBuilder.or;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.fcrepo.camel.processor.ProcessorUtils.tokenizePropertyPlaceholder;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A content router for handling JMS events.
 *
 * @author Aaron Coburn
 * @author escowles
 */
public class EventRouter extends RouteBuilder {

    private static final Logger LOGGER = getLogger(EventRouter.class);

    @Autowired
    private FcrepoAuditTriplestoreConfig config;

    /**
     * Configure the message route workflow.
     */
    public void configure() throws Exception {

        /**
         * A generic error handler (specific to this RouteBuilder)
         */
        onException(Exception.class)
            .maximumRedeliveries(config.getMaxRedeliveries())
            .log("Event Routing Error: ${routeId}");

        /**
         * Process a message.
         */
        from(config.getInputStream())
            .routeId("AuditFcrepoRouter")
            .process(new EventProcessor())
            .filter(not(in(tokenizePropertyPlaceholder(getContext(), config.getFilterContainers(), ",").stream()
                .map(uri -> or(
                    header(FCREPO_URI).startsWith(constant(uri + "/")),
                    header(FCREPO_URI).isEqualTo(constant(uri))))
                .collect(toList()))))
            .to("direct:event");

        from("direct:event")
            .routeId("AuditEventRouter")
            .setHeader(AuditHeaders.EVENT_BASE_URI, simple(config.getEventBaseUri()))
            .process(new AuditSparqlProcessor())
            .log(LoggingLevel.INFO, "org.fcrepo.camel.audit",
                "Audit Event: ${headers.CamelFcrepoUri} :: ${headers[CamelAuditEventUri]}")
            .process(new AddBasicAuthProcessor(this.config.getTriplestoreAuthUsername(),
                        this.config.getTriplestoreAuthPassword()))
            .to(config.getTriplestoreBaseUrl() + "?useSystemProperties=true");
    }
}
