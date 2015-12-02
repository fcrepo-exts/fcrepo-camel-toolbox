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
package org.fcrepo.camel.serialization;

import static org.apache.camel.LoggingLevel.INFO;
import static org.apache.camel.LoggingLevel.DEBUG;
import static org.apache.camel.Exchange.FILE_NAME;
import static org.apache.camel.builder.PredicateBuilder.not;
import static org.apache.camel.builder.PredicateBuilder.or;
import static org.apache.camel.component.exec.ExecBinding.EXEC_COMMAND_ARGS;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_IDENTIFIER;
import static org.fcrepo.camel.JmsHeaders.EVENT_TYPE;
import static org.fcrepo.camel.JmsHeaders.IDENTIFIER;
import static org.fcrepo.camel.RdfNamespaces.RDF;
import static org.fcrepo.camel.RdfNamespaces.REPOSITORY;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.Namespaces;
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

    private static final String isBinaryResourceXPath =
        "/rdf:RDF/rdf:Description/rdf:type[@rdf:resource='" + REPOSITORY + "Binary']";
    /**
     * Configure the message route workflow
     *
     */

    public void configure() throws Exception {

        final Namespaces ns = new Namespaces("rdf", RDF).add("fedora", REPOSITORY);

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
            .setHeader(FCREPO_IDENTIFIER).header(IDENTIFIER)
            .filter(not(or(header(FCREPO_IDENTIFIER).startsWith(simple("{{audit.container}}/")),
                    header(FCREPO_IDENTIFIER).isEqualTo(simple("{{audit.container}}")))))
            .choice()
                .when(header(EVENT_TYPE).isEqualTo(REPOSITORY + "NODE_REMOVED"))
                    .multicast().to("direct:delete.metadata", "direct:delete.binary").endChoice()
                .otherwise()
                    .multicast().to("direct:metadata", "direct:binary");

        from("{{serialization.stream}}")
            .routeId("FcrepoReSerialization")
            .filter(not(or(header(FCREPO_IDENTIFIER).startsWith(simple("{{audit.container}}/")),
                    header(FCREPO_IDENTIFIER).isEqualTo(simple("{{audit.container}}")))))
            .multicast().to("direct:metadata", "direct:binary");

        from("direct:metadata")
            .routeId("FcrepoSerializationMetadataUpdater")
            .to("fcrepo:{{fcrepo.baseUrl}}?accept={{serialization.mimeType}}")
            .log(INFO, LOGGER,
                    "Serializing object ${headers[CamelFcrepoIdentifier]}")
            .setHeader(FILE_NAME)
                .simple("${headers[CamelFcrepoIdentifier]}.{{serialization.extension}}")
            .log(DEBUG, LOGGER, "filename is ${headers[CamelFileName]}")
            .to("{{serialization.descriptions}}");

        from("direct:binary")
            .routeId("FcrepoSerializationBinaryUpdater")
            .filter().simple("{{serialization.includeBinaries}} == 'true'")
            .to("fcrepo:{{fcrepo.baseUrl}}?preferInclude=PreferMinimalContainer")
            .filter().xpath(isBinaryResourceXPath, ns)
            .log(INFO, LOGGER,
                    "Writing binary ${headers[CamelFcrepoIdentifier]}")
            .to("fcrepo:{{fcrepo.baseUrl}}?metadata=false")
            .setHeader(FILE_NAME).header(FCREPO_IDENTIFIER)
            .log(DEBUG, LOGGER, "header filename is: ${headers[CamelFileName]}")
            .to("{{serialization.binaries}}");

        from("direct:delete.metadata")
            .routeId("FcrepoSerializationMetadataDeleter")
            .setHeader(EXEC_COMMAND_ARGS).simple(
                    "{{serialization.descriptions}}/${headers[CamelFcrepoIdentifier]}{{serialization.extension}}")
            .to("exec:rm")
            .log(INFO, LOGGER,
                    "Deleting object from serialized backup ${headers[CamelFcrepoIdentifier]}");

        from("direct:delete.binary")
            .routeId("FcrepoSerializationBinaryDeleter")
            .filter().xpath(isBinaryResourceXPath, String.class, ns)
            .setHeader(EXEC_COMMAND_ARGS).simple(
                    "{{serialization.binaries}}${headers[CamelFcrepoIdentifier]}")
            .to("exec:rm")
            .log(INFO, LOGGER,
                    "Deleting binary from serialized backup ${headers[CamelFcrepoIdentifier]}");
    }
}
