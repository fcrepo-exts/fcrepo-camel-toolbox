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
package org.fcrepo.camel.indexing.triplestore.integration;

import static com.jayway.awaitility.Awaitility.await;
import static org.fcrepo.camel.RdfNamespaces.REPOSITORY;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.camel.util.ObjectHelper;
import org.apache.jena.fuseki.EmbeddedFusekiServer;
import org.fcrepo.camel.JmsHeaders;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.camel.FcrepoHeaders;
import org.fcrepo.client.FcrepoResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

/**
 * Test the route workflow.
 *
 * @author Aaron Coburn
 * @since 2015-04-10
 */
public class RouteDeleteIT extends CamelBlueprintTestSupport {

    final private Logger logger = getLogger(RouteDeleteIT.class);

    private static EmbeddedFusekiServer server = null;

    private String fullPath = "";

    private static final String FUSEKI_PORT = System.getProperty(
            "fuseki.dynamic.test.port", "8080");

    private static final String FCREPO_PORT = System.getProperty(
            "fcrepo.dynamic.test.port", "8080");

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    @Override
    protected void doPreSetup() throws Exception {
    }

    @Before
    public void setUpFuseki() throws Exception {
        final FcrepoClient client = new FcrepoClient(null, null, null, true);
        final FcrepoResponse res = client.post(
                URI.create("http://localhost:" + FCREPO_PORT + "/fcrepo/rest"),
                ObjectHelper.loadResourceAsStream("container.ttl"), "text/turtle");
        fullPath = res.getLocation().toString();

        logger.info("Starting EmbeddedFusekiServer on port {}", FUSEKI_PORT);
        server = EmbeddedFusekiServer.mem(Integer.parseInt(FUSEKI_PORT, 10), "/fuseki/test") ;
        server.start();
    }

    @After
    public void tearDownFuseki() throws Exception {
        logger.info("Stopping EmbeddedFusekiServer");
        server.stop();
    }

    @Override
    public boolean isUseAdviceWith() {
        return true;
    }

    @Override
    public boolean isUseRouteBuilder() {
        return false;
    }

    @Override
    protected String getBlueprintDescriptor() {
        return "/OSGI-INF/blueprint/blueprint-test.xml";
    }

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        final String jmsPort = System.getProperty("fcrepo.dynamic.jms.port", "61616");
        final Properties props = new Properties();
        props.put("indexing.predicate", "true");
        props.put("triplestore.baseUrl", "http://localhost:" + FUSEKI_PORT + "/fuseki/test/update");
        props.put("fcrepo.baseUrl", "http://localhost:" + FCREPO_PORT + "/fcrepo/rest");
        props.put("jms.brokerUrl", "tcp://localhost:" + jmsPort);
        props.put("input.stream", "direct:start");
        return props;
    }

    @Test
    public void testDeletedResourceWithJmsHeaders() throws Exception {
        final String path = fullPath.replaceFirst("http://localhost:[0-9]+/fcrepo/rest", "");
        final String fusekiEndpoint = "mock:http:localhost:" + FUSEKI_PORT + "/fuseki/test/update";
        final String fcrepoEndpoint = "mock:fcrepo:http://localhost:" + FCREPO_PORT + "/fcrepo/rest";
        final String fusekiBase = "http://localhost:" + FUSEKI_PORT + "/fuseki/test";

        context.getRouteDefinition("FcrepoTriplestoreRouter").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                mockEndpoints("*");
            }
        });
        context.getRouteDefinition("FcrepoTriplestoreUpdater").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                mockEndpoints("*");
            }
        });
        context.getRouteDefinition("FcrepoTriplestoreDeleter").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                mockEndpoints("*");
            }
        });

        TestUtils.populateFuseki(fusekiBase, fullPath);

        await().until(TestUtils.triplestoreCount(fusekiBase, fullPath), greaterThanOrEqualTo(1));

        context.start();

        final Map<String, Object> headers = new HashMap<>();
        headers.put(JmsHeaders.IDENTIFIER, path);
        headers.put(JmsHeaders.BASE_URL, "http://localhost:" + FCREPO_PORT + "/fcrepo/rest");
        headers.put(JmsHeaders.EVENT_TYPE, REPOSITORY + "NODE_REMOVED");
        headers.put(JmsHeaders.TIMESTAMP, 1428360320168L);
        headers.put(JmsHeaders.PROPERTIES, "");

        getMockEndpoint(fusekiEndpoint).expectedMessageCount(1);
        getMockEndpoint("mock://direct:delete.triplestore").expectedMessageCount(1);
        getMockEndpoint("mock://direct:update.triplestore").expectedMessageCount(0);
        getMockEndpoint(fcrepoEndpoint).expectedMessageCount(0);

        template.sendBodyAndHeaders("direct:start", "", headers);

        await().until(TestUtils.triplestoreCount(fusekiBase, fullPath), equalTo(0));

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testDeletedResourceWithFcrepoHeaders() throws Exception {
        final String path = fullPath.replaceFirst("http://localhost:[0-9]+/fcrepo/rest", "");
        final String fusekiEndpoint = "mock:http:localhost:" + FUSEKI_PORT + "/fuseki/test/update";
        final String fcrepoEndpoint = "mock:fcrepo:http://localhost:" + FCREPO_PORT + "/fcrepo/rest";
        final String fusekiBase = "http://localhost:" + FUSEKI_PORT + "/fuseki/test";

        context.getRouteDefinition("FcrepoTriplestoreRouter").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                mockEndpoints("*");
            }
        });
        context.getRouteDefinition("FcrepoTriplestoreDeleter").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                mockEndpoints("*");
            }
        });

        TestUtils.populateFuseki(fusekiBase, fullPath);

        await().until(TestUtils.triplestoreCount(fusekiBase, fullPath), greaterThanOrEqualTo(1));

        context.start();

        final Map<String, Object> headers = new HashMap<>();
        headers.put(FcrepoHeaders.FCREPO_IDENTIFIER, path);
        headers.put(FcrepoHeaders.FCREPO_BASE_URL, "http://localhost:" + FCREPO_PORT + "/fcrepo/rest");
        headers.put(JmsHeaders.EVENT_TYPE, REPOSITORY + "NODE_REMOVED");

        getMockEndpoint(fusekiEndpoint).expectedMessageCount(1);
        getMockEndpoint("mock://direct:delete.triplestore").expectedMessageCount(1);
        getMockEndpoint("mock://direct:update.triplestore").expectedMessageCount(0);
        getMockEndpoint(fcrepoEndpoint).expectedMessageCount(0);

        template.sendBodyAndHeaders("direct:start", "", headers);

        await().until(TestUtils.triplestoreCount(fusekiBase, fullPath), equalTo(0));

        assertMockEndpointsSatisfied();
    }
}
