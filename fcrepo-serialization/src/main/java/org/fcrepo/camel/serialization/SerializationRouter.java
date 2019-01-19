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

import static java.net.URI.create;
import static java.util.stream.Collectors.toList;
import static org.apache.camel.LoggingLevel.INFO;
import static org.apache.camel.LoggingLevel.DEBUG;
import static org.apache.camel.Exchange.FILE_NAME;
import static org.apache.camel.builder.PredicateBuilder.in;
import static org.apache.camel.builder.PredicateBuilder.not;
import static org.apache.camel.builder.PredicateBuilder.or;
import static org.apache.camel.component.exec.ExecBinding.EXEC_COMMAND_ARGS;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_EVENT_TYPE;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.fcrepo.camel.processor.ProcessorUtils.tokenizePropertyPlaceholder;

import java.util.List;

import org.apache.camel.Predicate;
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

    final String AUTH_PARAMS = "authUsername=" + System.getProperty("fcrepo.authUsername", "fedoraAdmin") +
            "&authPassword=" + System.getProperty("fcrepo.authPassword", "fedoraAdmin");

    private static final String RESOURCE_DELETION = "http://fedora.info/definitions/v4/event#ResourceDeletion";
    private static final String DELETE = "https://www.w3.org/ns/activitystreams#Delete";
    private static final String REPOSITORY = "http://fedora.info/definitions/v4/repository#";

    private static final String isBinaryResourceXPath =
        "/rdf:RDF/rdf:Description/rdf:type[@rdf:resource=\"" + REPOSITORY + "Binary\"]";

    public static final String SERIALIZATION_PATH = "CamelSerializationPath";

    public final List<Predicate> uriFilter = tokenizePropertyPlaceholder(getContext(), "{{filter.containers}}", ",")
                        .stream().map(uri -> or(
                            header(FCREPO_URI).startsWith(constant(uri + "/")),
                            header(FCREPO_URI).isEqualTo(constant(uri))))
                        .collect(toList());

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
            .process(exchange -> {
                final String uri = exchange.getIn().getHeader(FCREPO_URI, "", String.class);
                exchange.getIn().setHeader(SERIALIZATION_PATH, create(uri).getPath());
            })
            .filter(not(in(uriFilter)))
            .choice()
                .when(or(header(FCREPO_EVENT_TYPE).contains(RESOURCE_DELETION),
                            header(FCREPO_EVENT_TYPE).contains(DELETE)))
                    .to("direct:delete")
                .otherwise()
                    .multicast().to("direct:metadata", "direct:binary");

        from("{{serialization.stream}}")
            .routeId("FcrepoReSerialization")
            .filter(not(in(uriFilter)))
            .process(exchange -> {
                final String uri = exchange.getIn().getHeader(FCREPO_URI, "", String.class);
                exchange.getIn().setHeader(SERIALIZATION_PATH, create(uri).getPath());
            })
            .multicast().to("direct:metadata", "direct:binary");

        from("direct:metadata")
            .routeId("FcrepoSerializationMetadataUpdater")
            .to("fcrepo:localhost?accept={{serialization.mimeType}}&" + AUTH_PARAMS)
            .log(INFO, LOGGER, "Serializing object ${headers[CamelFcrepoUri]}")
            .setHeader(FILE_NAME).simple("${headers[CamelSerializationPath]}.{{serialization.extension}}")
            .log(DEBUG, LOGGER, "filename is ${headers[CamelFileName]}")
            .to("file://{{serialization.descriptions}}");

        from("direct:binary")
            .routeId("FcrepoSerializationBinaryUpdater")
            .filter().simple("{{serialization.includeBinaries}} == 'true'")
            .to("fcrepo:localhost?preferInclude=PreferMinimalContainer" +
                    "&accept=application/rdf+xml&" + AUTH_PARAMS)
            .filter().xpath(isBinaryResourceXPath, ns)
            .log(INFO, LOGGER, "Writing binary ${headers[CamelSerializationPath]}")
            .to("fcrepo:localhost?metadata=false&" + AUTH_PARAMS)
            .setHeader(FILE_NAME).header(SERIALIZATION_PATH)
            .log(DEBUG, LOGGER, "header filename is: ${headers[CamelFileName]}")
            .to("file://{{serialization.binaries}}");

        from("direct:delete")
            .routeId("FcrepoSerializationDeleter")
            .setHeader(EXEC_COMMAND_ARGS).simple(
                "-rf {{serialization.descriptions}}${headers[CamelSerializationPath]}.{{serialization.extension}} " +
                "{{serialization.descriptions}}${headers[CamelSerializationPath]} " +
                "{{serialization.binaries}}${headers[CamelSerializationPath]}")
            .to("exec:rm");
    }
}
