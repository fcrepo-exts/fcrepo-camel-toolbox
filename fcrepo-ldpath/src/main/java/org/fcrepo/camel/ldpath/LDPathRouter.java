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
package org.fcrepo.camel.ldpath;

import static java.lang.String.format;
import static org.apache.camel.builder.PredicateBuilder.and;
import static org.apache.camel.builder.PredicateBuilder.not;
import static org.apache.camel.model.dataformat.JsonLibrary.Jackson;
import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.Exchange.HTTP_METHOD;
import static org.apache.camel.Exchange.HTTP_RESPONSE_CODE;
import static org.apache.camel.Exchange.HTTP_URI;
import static org.slf4j.LoggerFactory.getLogger;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.support.builder.ExpressionBuilder;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A content router for an LDPath service.
 *
 * @author Aaron Coburn
 * @since Aug 5, 2016
 */

public class LDPathRouter extends RouteBuilder {

    private static final Logger LOGGER = getLogger(LDPathRouter.class);

    @Autowired
    private LDPathWrapper ldpathWrapper;

    @Autowired
    private FcrepoLdPathConfig config;
    /**
     * Configure the message route workflow.
     */
    public void configure() throws Exception {

        /**
         * Expose a RESTful endpoint for LDPath processing
         */
        from(format("jetty:http://%s:%d%s?" +
                "httpMethodRestrict=GET,POST,OPTIONS" +
                "&sendServerVersion=false", config.getRestHost(), config.getRestPort(),config.getRestPrefix()))
            .routeId("FcrepoLDPathRest")
            .routeDescription("Expose the ldpath endpoint over HTTP")
            .choice()
                .when(header(HTTP_METHOD).isEqualTo("OPTIONS"))
                    .setHeader(CONTENT_TYPE).constant("text/turtle")
                    .setHeader("Allow").constant("GET,POST,OPTIONS")
                    .to("language:simple:resource:classpath:org/fcrepo/camel/ldpath/options.ttl")
                // make sure the required context parameter is present
                .when(not(and(header("context").isNotNull(), header("context").regex("^https?://.+"))))
                    .setHeader(HTTP_RESPONSE_CODE).constant(400)
                    .setHeader(CONTENT_TYPE).constant("text/plain")
                    .transform(constant("Missing context parameter"))
                .when(header(HTTP_METHOD).isEqualTo("GET"))
                    .to("direct:get")
                .when(header(HTTP_METHOD).isEqualTo("POST"))
                    .to("direct:ldpathPrepare");

        from("direct:get")
            .routeId("FcrepoLDPathGet")
            .choice()
                .when(and(header("ldpath").isNotNull(), header("ldpath").regex("^https?://.*")))
                    .removeHeaders("CamelHttp*")
                    .setHeader(HTTP_URI).header("ldpath")
                    .to("http://localhost?useSystemProperties=true")
                    .to("direct:ldpathPrepare")
                .otherwise()
                    .to("language:simple:resource:classpath:org/fcrepo/camel/ldpath/default.ldpath")
                    .to("direct:ldpathPrepare");

        from("direct:ldpathPrepare").routeId("FcrepoLDPathPrepare")
            .to("direct:ldpath")
            .to("direct:format");

        from("direct:format").routeId("FcrepoLDPathFormat")
            .marshal().json(Jackson)
            .removeHeaders("*")
            .setHeader(CONTENT_TYPE).constant("application/json");

        from("direct:ldpath")
                .setBody(ExpressionBuilder.beanExpression(ldpathWrapper,
                        "programQuery(${headers.context}, ${body})"));
    }
}
