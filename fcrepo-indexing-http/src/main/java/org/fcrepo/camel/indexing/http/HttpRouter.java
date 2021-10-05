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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.support.builder.Namespaces;
import org.fcrepo.camel.processor.EventProcessor;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import static org.apache.camel.Exchange.HTTP_METHOD;
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
            .to("direct:send.to.http");

        /*
         * Handle re-index events
         */
        from(config.getReindexStream())
            .routeId("FcrepoHttpReindex")
            .to("direct:send.to.http");

        /*
         * Forward message to Http
         */
        from("direct:send.to.http").routeId("FcrepoHttpSend")
            .log(LoggingLevel.INFO, LOGGER, "sending ${headers[CamelFcrepoUri]} to http endpoint...")
            .to("mustache:org/fcrepo/camel/indexing/http/reindex.mustache")
            .setHeader(HTTP_METHOD).constant("POST")
            .to(config.getHttpBaseUrl());
    }
}
