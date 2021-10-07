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

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ModelCamelContext;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockserver.model.HttpRequest;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.verify.VerificationTimes;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.net.URI;
import java.util.Properties;

import static java.lang.Integer.parseInt;
import static org.apache.camel.util.ObjectHelper.loadResourceAsStream;
import static org.fcrepo.camel.indexing.http.integration.TestUtils.ASSERT_PERIOD_MS;
import static org.fcrepo.camel.indexing.http.integration.TestUtils.createClient;
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

    private static ClientAndServer server = null;

    private static final String AS_NS = "https://www.w3.org/ns/activitystreams#";

    private String fullPath = "";

    private static final String MOCKSERVER_PORT = System.getProperty(
            "mockserver.dynamic.test.port", "8080");

    private static final String FCREPO_PORT = System.getProperty(
            "fcrepo.dynamic.test.port", "8080");

    @EndpointInject("mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce("direct:start")
    protected ProducerTemplate template;

    @Autowired
    private CamelContext camelContext;

    @BeforeClass
    public static void beforeClass() {
        final String jmsPort = System.getProperty("fcrepo.dynamic.jms.port", "61616");
        final Properties props = new Properties();
        System.setProperty("http.indexer.enabled", "true");
        System.setProperty("http.baseUrl", "http://localhost:" + MOCKSERVER_PORT + "/endpoint");
        System.setProperty("fcrepo.baseUrl", "http://localhost:" + FCREPO_PORT + "/fcrepo/rest");
        System.setProperty("jms.brokerUrl", "tcp://localhost:" + jmsPort);
        System.setProperty("http.input.stream", "direct:start");
    }

    @After
    public void tearDownMockServer() throws Exception {
        logger.info("Stopping MockServer");
        server.stop();
    }

    @Before
    public void setUpMockServer() throws Exception {
        server = ClientAndServer.startClientAndServer(parseInt(MOCKSERVER_PORT));
    }

    @DirtiesContext
    @Test
    public void testDeletedResourceWithEventBody() throws Exception {
        final String path = fullPath.replaceFirst("http://localhost:[0-9]+/fcrepo/rest", "");
        final String fcrepoEndpoint = "mock:fcrepo:http://localhost:" + FCREPO_PORT + "/fcrepo/rest";
        final String mockServerBase = "http://localhost:" + MOCKSERVER_PORT + "/endpoint";
        final String mockServerEndpoint = "mock:http:localhost:" + MOCKSERVER_PORT + "/endpoint";

        final var context = camelContext.adapt(ModelCamelContext.class);

        server.verify(
            HttpRequest.request()
                .withMethod("POST")
                .withPath("/endpoint")
                .withBody("{id: \"foo\", type: \"bar\"}"),
            VerificationTimes.exactly(1)
        );

        AdviceWith.adviceWith(context, "FcrepoHttpRouter", a -> {
            a.mockEndpoints("*");
        });

        AdviceWith.adviceWith(context, "FcrepoHttpAddType", a -> {
            a.mockEndpoints("*");
        });

        AdviceWith.adviceWith(context, "FcrepoHttpSend", a -> {
            a.mockEndpoints("*");
        });

        final var mockServerMockEndpoint = MockEndpoint.resolve(camelContext, mockServerEndpoint);
        mockServerMockEndpoint.expectedMessageCount(1);

        final var updateEndpoint = MockEndpoint.resolve(camelContext, "mock://direct:send.to.http");
        updateEndpoint.expectedMessageCount(1);
        updateEndpoint.setAssertPeriod(ASSERT_PERIOD_MS);
        final var fcrepoMockEndpoint = MockEndpoint.resolve(camelContext, fcrepoEndpoint);
        fcrepoMockEndpoint.expectedMessageCount(0);
        fcrepoMockEndpoint.setAssertPeriod(ASSERT_PERIOD_MS);
        template.sendBody("direct:start", getEvent(fullPath, AS_NS + "Delete"));

        MockEndpoint.assertIsSatisfied(mockServerMockEndpoint, fcrepoMockEndpoint, updateEndpoint);
    }
}
