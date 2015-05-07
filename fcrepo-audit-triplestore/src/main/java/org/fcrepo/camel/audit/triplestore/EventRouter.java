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
package org.fcrepo.camel.audit.triplestore;

import static org.fcrepo.camel.JmsHeaders.IDENTIFIER;
import static org.apache.camel.builder.PredicateBuilder.not;
import static org.apache.camel.builder.PredicateBuilder.or;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;

/**
 * A content router for handling JMS events.
 *
 * @author Aaron Coburn
 * @author escowles
 */
public class EventRouter extends RouteBuilder {

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
            .filter(not(or(header(IDENTIFIER).startsWith(simple("{{audit.container}}/")),
                           header(IDENTIFIER).isEqualTo(simple("{{audit.container}}")))))
                .to("direct:event");

        from("direct:event")
            .routeId("AuditEventRouter")
            .setHeader(AuditHeaders.EVENT_BASE_URI, simple("{{event.baseUri}}"))
            .process(new AuditSparqlProcessor())
            .log(LoggingLevel.INFO, "org.fcrepo.camel.audit",
                    "Audit Event: ${headers[org.fcrepo.jms.identifier]} :: ${headers[CamelAuditEventUri]}")
            .to("http4:{{triplestore.baseUrl}}");
    }
}
