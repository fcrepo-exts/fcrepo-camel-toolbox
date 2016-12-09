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
package org.fcrepo.camel.audit.triplestore;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.apache.camel.builder.PredicateBuilder.in;
import static org.apache.camel.builder.PredicateBuilder.not;
import static org.apache.camel.builder.PredicateBuilder.or;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;

import org.apache.camel.LoggingLevel;
import org.apache.camel.Predicate;
import org.apache.camel.builder.RouteBuilder;
import org.fcrepo.camel.processor.EventProcessor;
import org.slf4j.Logger;

/**
 * A content router for handling JMS events.
 *
 * @author Aaron Coburn
 * @author escowles
 */
public class EventRouter extends RouteBuilder {

    private static final Logger LOGGER = getLogger(EventRouter.class);

    /**
     * Configure the message route workflow.
     */
    public void configure() throws Exception {

        /**
         * A generic error handler (specific to this RouteBuilder)
         */
        onException(Exception.class)
            .maximumRedeliveries("{{error.maxRedeliveries}}")
            .log("Event Routing Error: ${routeId}");

        /**
         * Process a message.
         */
        from("{{input.stream}}")
            .routeId("AuditFcrepoRouter")
            .process(new EventProcessor())
            .filter(not(in(getUriFilter())))
                .to("direct:event");

        from("direct:event")
            .routeId("AuditEventRouter")
            .setHeader(AuditHeaders.EVENT_BASE_URI, simple("{{event.baseUri}}"))
            .process(new AuditSparqlProcessor())
            .log(LoggingLevel.INFO, "org.fcrepo.camel.audit",
                    "Audit Event: ${headers.CamelFcrepoUri} :: ${headers[CamelAuditEventUri]}")
            .to("{{triplestore.baseUrl}}?useSystemProperties=true");
    }

    private List<Predicate> getUriFilter() {
        try {
            return stream(getContext().resolvePropertyPlaceholders("{{filter.containers}}").split("\\s*,\\s*"))
                    .map(uri -> or(
                            header(FCREPO_URI).startsWith(constant(uri + "/")),
                            header(FCREPO_URI).isEqualTo(constant(uri))))
                    .collect(toList());
        } catch (final Exception ex) {
            LOGGER.debug("No filter containers were defined");
            return emptyList();
        }
    }
}
