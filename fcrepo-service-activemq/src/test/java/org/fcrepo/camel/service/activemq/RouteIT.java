/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel.service.activemq;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.ServiceStatus;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.fcrepo.camel.common.config.CamelConfiguration;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.fcrepo.camel.processor.EventProcessor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.apache.camel.test.spring.junit6.CamelSpringTest;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.camel.component.mock.MockEndpoint.assertIsSatisfied;
import static org.apache.hc.core5.http.HttpStatus.SC_CREATED;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Test the route workflow.
 *
 * @author Aaron Coburn
 * @since 2016-05-04
 */
@CamelSpringTest
@ContextConfiguration(classes = {ContextConfig.class}, loader = AnnotationConfigContextLoader.class)
public class RouteIT {
    private static final Logger LOGGER = getLogger(RouteIT.class);
    private static String FEDORA_USERNAME = "fedoraAdmin";
    private static String FEDORA_PASSWORD = "fedoraAdmin";

    @EndpointInject("mock:result")
    protected MockEndpoint resultEndpoint;

    @Autowired
    private CamelContext camelContext;

    @BeforeAll
    public static void beforeClass() {
        final String jmsPort = System.getProperty("fcrepo.dynamic.jms.port", "61616");
        System.setProperty("jms.brokerUrl", "tcp://localhost:" + jmsPort);
    }

    @Test
    public void testQueuingService() throws Exception {
        assertEquals(ServiceStatus.Started, camelContext.getStatus());
        final String webPort = System.getProperty("fcrepo.dynamic.test.port", "8080");
        final String baseUrl = "http://localhost:" + webPort + "/fcrepo/rest";

        resultEndpoint.reset();

        final String url1 = post(baseUrl);
        final String url2 = post(baseUrl);

        final List<String> expectedIds = new ArrayList<>();
        expectedIds.add(url1);
        expectedIds.add(url2);

        // expectedMessageCount is set to the number of elements passed to the below function,
        // so we need to account for them all or the test stops and just checks the ones we have.
        resultEndpoint.expectedHeaderValuesReceivedInAnyOrder(FCREPO_URI, expectedIds);
        resultEndpoint.await(500, TimeUnit.MILLISECONDS);

        assertIsSatisfied(resultEndpoint);
    }

    private String post(final String url) {
        final BasicCredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(new AuthScope(null, -1),
                new UsernamePasswordCredentials(FEDORA_USERNAME, FEDORA_PASSWORD.toCharArray()));
        final CloseableHttpClient httpclient = HttpClients.custom().setDefaultCredentialsProvider(provider).build();
        try {
            final HttpPost httppost = new HttpPost(url);
            return httpclient.execute(httppost, response -> {
                assertEquals(SC_CREATED, response.getCode());
                return EntityUtils.toString(response.getEntity(), UTF_8);
            });
        } catch (final IOException ex) {
            LOGGER.debug("Unable to extract HttpEntity response into an InputStream: ", ex);
            return "";
        }
    }
}

@Configuration
@ComponentScan("org.fcrepo.camel")
class ContextConfig extends CamelConfiguration {

    @Bean
    public RouteBuilder route() {
        return new RouteBuilder() {
            public void configure() throws Exception {
                from("broker:topic:fedora")
                        .process(new EventProcessor())
                        .to("mock:result");
            }
        };
    }
}
