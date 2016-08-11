/*
 * Copyright 2016 DuraSpace, Inc.
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
package org.fcrepo.camel.ldpath;

import static org.apache.camel.builder.PredicateBuilder.and;
import static org.apache.camel.model.dataformat.JsonLibrary.Jackson;
import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.Exchange.HTTP_METHOD;
import static org.apache.camel.Exchange.HTTP_URI;
import static org.slf4j.LoggerFactory.getLogger;

import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;

/**
 * A content router for an LDPath service.
 *
 * @author Aaron Coburn
 * @since Aug 5, 2016
 */
public class LDPathRouter extends RouteBuilder {

    private static final Logger LOGGER = getLogger(LDPathRouter.class);

    public static final String FEDORA_URI = "CamelFedoraUri";

    /**
     * Configure the message route workflow.
     */
    public void configure() throws Exception {

        /**
         * Expose a RESTful endpoint for LDPath processing
         */
        from("jetty:http://{{rest.host}}:{{rest.port}}{{rest.prefix}}" +
                "?matchOnUriPrefix=true" +
                "&httpMethodRestrict=GET,POST,OPTIONS" +
                "&sendServerVersion=false")
            .routeId("FcrepoLDPathRest")
            .routeDescription("Expose the ldpath endpoint over HTTP")
            .setHeader(FEDORA_URI).simple("{{fcrepo.baseUrl}}${headers.CamelHttpPath}")
            .choice()
                .when(header(HTTP_METHOD).isEqualTo("GET"))
                    .to("direct:get")
                .when(header(HTTP_METHOD).isEqualTo("POST"))
                    .to("direct:ldpathPrepare")
                .when(header(HTTP_METHOD).isEqualTo("OPTIONS"))
                    .setHeader(CONTENT_TYPE).constant("text/turtle")
                    .setHeader("Allow").constant("GET,POST,OPTIONS")
                    .to("language:simple:resource:classpath:options.ttl");

        from("direct:get")
            .routeId("FcrepoLDPathGet")
            .choice()
                .when(and(header("ldpath").isNotNull(), header("ldpath").regex("^https?://.*")))
                    .setHeader(HTTP_URI).header("ldpath")
                    .to("http4://localhost?useSystemProperties=true")
                    .to("direct:ldpathPrepare")
                .otherwise()
                    .to("language:simple:resource:classpath:default.ldpath")
                    .to("direct:ldpathPrepare");

        from("direct:ldpathPrepare").routeId("FcrepoLDPathPrepare")
            .to("direct:ldpath")
            .to("direct:format");

        from("direct:format").routeId("FcrepoLDPathFormat")
            .marshal().json(Jackson)
            .removeHeaders("*")
            .setHeader(CONTENT_TYPE).constant("application/json");
    }
}
