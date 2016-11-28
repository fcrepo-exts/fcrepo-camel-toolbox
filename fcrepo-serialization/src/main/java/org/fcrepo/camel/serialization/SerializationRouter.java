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
package org.fcrepo.camel.serialization;

import static org.apache.camel.LoggingLevel.INFO;
import static org.apache.camel.LoggingLevel.DEBUG;
import static org.apache.camel.Exchange.FILE_NAME;
import static org.apache.camel.builder.PredicateBuilder.not;
import static org.apache.camel.builder.PredicateBuilder.or;
import static org.apache.camel.component.exec.ExecBinding.EXEC_COMMAND_ARGS;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_BASE_URL;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_EVENT_TYPE;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_IDENTIFIER;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.Namespaces;
import org.fcrepo.camel.processor.EventProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * A router for serializing fedora objects and binaries.
 *
 * @author Bethany Seeger
 * @since 2015-09-28
 */

public class SerializationRouter extends RouteBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(SerializationRouter.class);

    private static final String RESOURCE_DELETION = "http://fedora.info/definitions/v4/event#ResourceDeletion";
    private static final String DELETE = "https://www.w3.org/ns/activitystreams#Delete";
    private static final String REPOSITORY = "http://fedora.info/definitions/v4/repository#";

    private static final String isBinaryResourceXPath =
        "/rdf:RDF/rdf:Description/rdf:type[@rdf:resource=\"" + REPOSITORY + "Binary\"]";
    /**
     * Configure the message route workflow
     *
     */

    public void configure() throws Exception {

        final Namespaces ns = new Namespaces("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#")
                .add("fedora", REPOSITORY);

        /**
         * A generic error handler (specific to this RouteBuilder)
         */
        onException(Exception.class)
                .maximumRedeliveries("{{error.maxRedeliveries}}")
                .log("Index Routing Error: ${routeId}");

        /**
         * Handle Serialization Events
         */
        from("{{input.stream}}")
            .routeId("FcrepoSerialization")
            .process(new EventProcessor())
            // this is a hard dependency on the jms module and should be reworked
            .setHeader(FCREPO_IDENTIFIER).header("org.fcrepo.jms.identifier")
            .filter(not(or(header(FCREPO_URI).startsWith(simple("{{audit.container}}/")),
                    header(FCREPO_URI).isEqualTo(simple("{{audit.container}}")))))
            .choice()
                .when(or(header(FCREPO_EVENT_TYPE).contains(RESOURCE_DELETION),
                            header(FCREPO_EVENT_TYPE).contains(DELETE)))
                    .to("direct:delete")
                .otherwise()
                    .multicast().to("direct:metadata", "direct:binary");

        from("{{serialization.stream}}")
            .routeId("FcrepoReSerialization")
            .filter(not(or(header(FCREPO_URI).startsWith(simple("{{audit.container}}/")),
                    header(FCREPO_URI).isEqualTo(simple("{{audit.container}}")))))
            .process(exchange -> {
                final String baseUrl = exchange.getIn().getHeader(FCREPO_BASE_URL, "", String.class);
                final String uri = exchange.getIn().getHeader(FCREPO_URI, "", String.class);
                exchange.getIn().setHeader(FCREPO_IDENTIFIER, uri.replaceAll(baseUrl, ""));
            })
            .multicast().to("direct:metadata", "direct:binary");

        from("direct:metadata")
            .routeId("FcrepoSerializationMetadataUpdater")
            .to("fcrepo:{{fcrepo.baseUrl}}?accept={{serialization.mimeType}}")
            .log(INFO, LOGGER,
                    "Serializing object ${headers[CamelFcrepoIdentifier]}")
            .setHeader(FILE_NAME)
                .simple("${headers[CamelFcrepoIdentifier]}.{{serialization.extension}}")
            .log(DEBUG, LOGGER, "filename is ${headers[CamelFileName]}")
            .to("file://{{serialization.descriptions}}");

        from("direct:binary")
            .routeId("FcrepoSerializationBinaryUpdater")
            .filter().simple("{{serialization.includeBinaries}} == 'true'")
            .to("fcrepo:{{fcrepo.baseUrl}}?preferInclude=PreferMinimalContainer" +
                    "&accept=application/rdf+xml")
            .filter().xpath(isBinaryResourceXPath, ns)
            .log(INFO, LOGGER, "Writing binary ${headers[CamelFcrepoIdentifier]}")
            .to("fcrepo:{{fcrepo.baseUrl}}?metadata=false")
            .setHeader(FILE_NAME).header(FCREPO_IDENTIFIER)
            .log(DEBUG, LOGGER, "header filename is: ${headers[CamelFileName]}")
            .to("file://{{serialization.binaries}}");

        from("direct:delete")
            .routeId("FcrepoSerializationDeleter")
            .setHeader(EXEC_COMMAND_ARGS).simple(
                    "-rf {{serialization.descriptions}}${headers[CamelFcrepoIdentifier]}.{{serialization.extension}} " +
                    "{{serialization.descriptions}}${headers[CamelFcrepoIdentifier]} " +
                    "{{serialization.binaries}}${headers[CamelFcrepoIdentifier]}")
            .to("exec:rm");
    }
}
