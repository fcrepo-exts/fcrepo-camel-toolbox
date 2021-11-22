/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel.service;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.javaconfig.CamelConfiguration;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.fcrepo.camel.processor.EventProcessor;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.apache.camel.component.mock.MockEndpoint.assertIsSatisfied;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_BASE_URL;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_IDENTIFIER;
import static org.junit.Assert.assertEquals;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Test the route workflow.
 *
 * @author Aaron Coburn
 * @since 2016-07-21
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ContextConfig.class}, loader = AnnotationConfigContextLoader.class)
public class RouteIT {

    private static final Logger LOGGER = getLogger(RouteIT.class);

    private static String FEDORA_USERNAME = "fedoraAdmin";
    private static String FEDORA_PASSWORD = "fedoraAdmin";
    private static String FEDORA_BASE_URL = "";
    @EndpointInject("mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce("direct:start")
    protected ProducerTemplate template;

    @BeforeClass
    public static void beforeClass() {
        System.setProperty("fcrepo.authUsername", FEDORA_USERNAME);
        System.setProperty("fcrepo.authPassword", FEDORA_PASSWORD);
        final String fcrepoPort = System.getProperty("fcrepo.dynamic.test.port", "8080");
        FEDORA_BASE_URL = "http://localhost:" + fcrepoPort + "/fcrepo/rest";
        System.setProperty("fcrepo.baseUrl", FEDORA_BASE_URL);
    }

    @Test
    public void testFcrepoService() throws Exception {
        final String webPort = System.getProperty("fcrepo.dynamic.test.port", "8080");

        final String baseUrl = "http://localhost:" + webPort + "/fcrepo/rest";
        final String url1 = post(baseUrl).replace(baseUrl, "");
        final String url2 = post(baseUrl).replace(baseUrl, "");

        final Map<String, Object> headers = new HashMap<>();
        headers.put(FCREPO_BASE_URL, baseUrl);
        headers.put(FCREPO_IDENTIFIER, url1);
        template.sendBodyAndHeaders(null, headers);

        headers.put(FCREPO_IDENTIFIER, url2);
        template.sendBodyAndHeaders(null, headers);

        resultEndpoint.expectedMessageCount(2);
        assertIsSatisfied(resultEndpoint);
    }

    private String post(final String url) {
        try {
            final BasicCredentialsProvider provider = new BasicCredentialsProvider();
            provider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(FEDORA_USERNAME, FEDORA_PASSWORD));
            final CloseableHttpClient httpclient = HttpClients.custom().setDefaultCredentialsProvider(provider).build();

            final HttpPost httppost = new HttpPost(url);
            final HttpResponse response = httpclient.execute(httppost);
            assertEquals(SC_CREATED, response.getStatusLine().getStatusCode());
            return EntityUtils.toString(response.getEntity(), "UTF-8");
        } catch (IOException ex) {
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
                from("direct:start")
                        .process(new EventProcessor())
                        .to("fcrepo:localhost/rest")
                        .to("mock:result");
            }
        };
    }
}
