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
package org.fcrepo.camel.indexing.http.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ModelCamelContext;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.lang.Integer.parseInt;
import static org.fcrepo.camel.indexing.http.integration.TestUtils.getEvent;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Test the route workflow.
 *
 * @author Aaron Coburn
 * @author Demian Katz
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {RouteUpdateIT.ContextConfig.class}, loader = AnnotationConfigContextLoader.class)
public class RouteDeleteIT {

    final private Logger logger = getLogger(RouteDeleteIT.class);

    private static final String AS_NS = "https://www.w3.org/ns/activitystreams#";

    private static final String MOCK_ENDPOINT= "/endpoint";

    private static final String MOCKSERVER_PORT = System.getProperty(
            "mockserver.dynamic.test.port", "8080");

    private static final String FCREPO_PORT = System.getProperty(
            "fcrepo.dynamic.test.port", "8080");

    private static final String BASIC_AUTH_USERNAME = "fooUser";

    private static final String BASIC_AUTH_PASSWORD = "barPass";

    private String fullPath = "http://localhost:" + FCREPO_PORT + "/fcrepo/rest/" + "fake-identifier";

    @EndpointInject("mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce("direct:start")
    protected ProducerTemplate template;

    @Autowired
    private CamelContext camelContext;

    private WireMockServer mockServer;

    @BeforeClass
    public static void beforeClass() {
        final String jmsPort = System.getProperty("fcrepo.dynamic.jms.port", "61616");
        System.setProperty("http.indexer.enabled", "true");
        System.setProperty("http.baseUrl", "http://localhost:" + MOCKSERVER_PORT + MOCK_ENDPOINT);
        System.setProperty("http.authUsername", BASIC_AUTH_USERNAME);
        System.setProperty("http.authPassword", BASIC_AUTH_PASSWORD);
        System.setProperty("fcrepo.baseUrl", "http://localhost:" + FCREPO_PORT + "/fcrepo/rest");
        System.setProperty("jms.brokerUrl", "tcp://localhost:" + jmsPort);
        System.setProperty("http.input.stream", "direct:start");
        System.setProperty("error.maxRedeliveries", "1");
    }

    @After
    public void tearDownMockServer() throws Exception {
        logger.info("Stopping HTTP Server");
        mockServer.stop();
    }

    @Before
    public void setUpMockServer() throws Exception {
        mockServer = new WireMockServer(WireMockConfiguration.options().port(parseInt(MOCKSERVER_PORT)));
        mockServer.start();
    }

    @DirtiesContext
    @Test
    public void testDeletedResourceWithEventBody() throws Exception {
        final var mockServerEndpoint = "mock:http:localhost:" + MOCKSERVER_PORT + MOCK_ENDPOINT;
        final var idMatcher = WireMock.matchingJsonPath("$.id", equalTo(fullPath));
        final var typeMatcher = WireMock.matchingJsonPath("$.type", equalTo(AS_NS + "Delete"));

        // have the http server return a 200; also test that basic auth works (for variety)
        mockServer.stubFor(post(urlEqualTo(MOCK_ENDPOINT))
            .withBasicAuth(BASIC_AUTH_USERNAME, BASIC_AUTH_PASSWORD)
            .willReturn(ok()));

        final var context = camelContext.adapt(ModelCamelContext.class);

        AdviceWith.adviceWith(context, "FcrepoHttpRouter", a -> a.mockEndpoints("*"));
        AdviceWith.adviceWith(context, "FcrepoHttpAddType", a -> a.mockEndpoints("*"));
        AdviceWith.adviceWith(context, "FcrepoHttpSend", a -> a.mockEndpoints("*"));

        final var mockServerMockEndpoint = MockEndpoint.resolve(camelContext, mockServerEndpoint);
        mockServerMockEndpoint.expectedMessageCount(1);
        final var updateEndpoint = MockEndpoint.resolve(camelContext, "mock://direct:send.to.http");
        updateEndpoint.expectedMessageCount(1);

        logger.info("fullPath={}", fullPath);
        template.sendBodyAndHeader("direct:start", getEvent(fullPath, AS_NS + "Delete"), "org.fcrepo.jms.eventtype", AS_NS + "Delete");

        mockServer.verify(1, postRequestedFor(urlEqualTo(MOCK_ENDPOINT))
            .withRequestBody(idMatcher.and(typeMatcher)));
        MockEndpoint.assertIsSatisfied(mockServerMockEndpoint, updateEndpoint);
    }
}
