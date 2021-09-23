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
package org.fcrepo.camel.fixity;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.support.builder.Namespaces;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * A content router for checking fixity of Binary resources.
 *
 * @author Aaron Coburn
 * @since 2015-06-18
 */
public class FixityRouter extends RouteBuilder {

    private static final Logger LOGGER = getLogger(FixityRouter.class);

    private static final String REPOSITORY = "http://fedora.info/definitions/v4/repository#";

    @Autowired
    private FcrepoFixityConfig config;

    /**
     * Configure the message route workflow.
     */
    public void configure() throws Exception {

        final Namespaces ns = new Namespaces("rdf", RDF.uri);
        ns.add("premis", "http://www.loc.gov/premis/rdf/v1#");

        /**
         * A generic error handler (specific to this RouteBuilder)
         */
        onException(Exception.class)
                .maximumRedeliveries(config.getMaxRedeliveries())
                .log("Index Routing Error: ${routeId}");

        /**
         * Handle fixity events
         */
        from(config.getInputStream())
                .routeId("FcrepoFixity")
                .to("fcrepo:" + config.getFcrepoBaseUrl() + "?preferInclude=ServerManged&accept=application/rdf+xml")
                .filter().xpath(
                "/rdf:RDF/rdf:Description/rdf:type" +
                        "[@rdf:resource='" + REPOSITORY + "Binary']", ns)
                .log(LoggingLevel.INFO, LOGGER,
                        "Checking Fixity for ${headers[CamelFcrepoUri]}")
                .delay(simple(String.valueOf(config.getFixityDelay())))
                .to("fcrepo:" + config.getFcrepoBaseUrl() + "?fixity=true&accept=application/rdf+xml")
                .choice()
                .when().xpath(
                "/rdf:RDF/rdf:Description/premis:hasEventOutcome" +
                        "[text()='SUCCESS']", ns)
                .to(config.getFixitySuccess())
                .otherwise()
                .log(LoggingLevel.WARN, LOGGER,
                        "Fixity error on ${headers[CamelFcrepoUri]}")
                .to(config.getFixityFailure());
    }
}
