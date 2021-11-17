/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel.httpforwarding.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.activemq.ActiveMQComponent;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.spring.javaconfig.CamelConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
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
import static org.fcrepo.camel.httpforwarding.integration.TestUtils.getEvent;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Test the route workflow.
 *
 * @author Aaron Coburn
 * @since 2015-04-10
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {RouteUpdateIT.ContextConfig.class}, loader = AnnotationConfigContextLoader.class)
public class RouteUpdateIT {

    final private Logger logger = getLogger(RouteUpdateIT.class);

    private static final String AS_NS = "https://www.w3.org/ns/activitystreams#";

    private static final String MOCK_ENDPOINT = "/endpoint";

    private static final String MOCKSERVER_PORT = System.getProperty(
            "mockserver.dynamic.test.port", "8080");

    private static final String FCREPO_PORT = System.getProperty(
            "fcrepo.dynamic.test.port", "8080");

    private static final String JMS_PORT = System.getProperty(
            "fcrepo.dynamic.jms.port", "61616");

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
        System.setProperty("http.enabled", "true");
        System.setProperty("http.baseUrl", "http://localhost:" + MOCKSERVER_PORT + MOCK_ENDPOINT);
        System.setProperty("jms.brokerUrl", "tcp://localhost:" + JMS_PORT);
        System.setProperty("http.input.stream", "direct:start");
        System.setProperty("http.reindex.stream", "direct:reindex");
        System.setProperty("fcrepo.baseUrl", "http://localhost:" + FCREPO_PORT + "/fcrepo/rest");
        System.setProperty("error.maxRedeliveries", "1");
    }

    @After
    public void tearDownMockServer() {
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
    public void testAddedEventRouter() throws Exception {
        final var mockServerEndpoint = "mock:http:localhost:" + MOCKSERVER_PORT + MOCK_ENDPOINT;
        final var idMatcher = WireMock.matchingJsonPath("$.id", equalTo(fullPath));
        final var typeMatcher = WireMock.matchingJsonPath("$.type", equalTo(AS_NS + "Update"));

        // have the http server return a 200
        mockServer.stubFor(post(urlEqualTo(MOCK_ENDPOINT)).willReturn(ok()));

        final var context = camelContext.adapt(ModelCamelContext.class);

        AdviceWith.adviceWith(context, "FcrepoHttpRouter", a -> a.mockEndpoints("*"));
        AdviceWith.adviceWith(context, "FcrepoHttpAddType", a -> a.mockEndpoints("*"));
        AdviceWith.adviceWith(context, "FcrepoHttpSend", a -> a.mockEndpoints("*"));

        final var mockServerMockEndpoint = MockEndpoint.resolve(camelContext, mockServerEndpoint);
        mockServerMockEndpoint.expectedMessageCount(1);
        final var updateEndpoint = MockEndpoint.resolve(camelContext, "mock://direct:send.to.http");
        updateEndpoint.expectedMessageCount(1);

        logger.info("fullPath={}", fullPath);
        template.sendBodyAndHeader(
            "direct:start", getEvent(fullPath, AS_NS + "Update"), "org.fcrepo.jms.eventtype", AS_NS + "Update"
        );

        mockServer.verify(1, postRequestedFor(urlEqualTo(MOCK_ENDPOINT))
            .withRequestBody(idMatcher.and(typeMatcher)));
        MockEndpoint.assertIsSatisfied(mockServerMockEndpoint, updateEndpoint);
    }

    @Configuration
    @ComponentScan(basePackages = "org.fcrepo.camel")
    static class ContextConfig extends CamelConfiguration {
        @Bean
        public ActiveMQComponent broker() {
            final var component = new ActiveMQComponent();
            component.setBrokerURL("tcp://localhost:" + JMS_PORT);
            return component;
        }
    }
}
