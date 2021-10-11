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
package org.fcrepo.camel.indexing.http;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
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

import java.util.Base64;

/**
 * A content router for handling JMS events.
 *
 * @author Geoff Scholl
 * @author Demian Katz
 */
public class HttpRouter extends RouteBuilder {

    private static final Logger LOGGER = getLogger(HttpRouter.class);

    @Autowired
    private FcrepoHttpIndexerConfig config;

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
            .to("mustache:org/fcrepo/camel/indexing/http/httpMessage.mustache")
            .setHeader(HTTP_METHOD).constant("POST")
            .setHeader(CONTENT_TYPE).constant("application/json")
            .choice()
                .when((x) -> !config.getHttpAuthUsername().isEmpty())
                .setHeader("Authorization", simple(
                    "Basic " + Base64.getEncoder().encodeToString(
                        (config.getHttpAuthUsername() + ":" + config.getHttpAuthPassword()).getBytes()))
                    )
                    .end()
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
