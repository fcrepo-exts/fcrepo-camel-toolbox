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
package org.fcrepo.camel.reindexing.integration;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.activemq.ActiveMQComponent;
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
import org.fcrepo.camel.reindexing.ReindexingRouter;
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

import java.io.IOException;

import static org.apache.camel.Exchange.HTTP_URI;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.fcrepo.camel.reindexing.ReindexingHeaders.REINDEXING_PREFIX;
import static org.fcrepo.camel.reindexing.ReindexingHeaders.REINDEXING_RECIPIENTS;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Test the route workflow.
 *
 * @author Aaron Coburn
 * @since 2015-04-10
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {RouteIT.ContextConfig.class}, loader = AnnotationConfigContextLoader.class)
public class RouteIT {

    private static final Logger LOGGER = getLogger(RouteIT.class);

    private static final String FEDORA_AUTH_USERNAME = "fedoraAdmin";
    private static final String FEDORA_AUTH_PASSWORD = "fedoraAdmin";

    @EndpointInject("mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce("direct:reindex")
    protected ProducerTemplate template;

    @Autowired
    private ActiveMQComponent activeMQComponent;

    private final String fullPath = "";

    private static final BasicCredentialsProvider provider = new BasicCredentialsProvider();

    public RouteIT() {
        provider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(FEDORA_AUTH_USERNAME, FEDORA_AUTH_PASSWORD));
    }

    @BeforeClass
    public static void beforeClass() {

        final String jmsPort = System.getProperty("fcrepo.dynamic.jms.port", "61616");
        final String webPort = System.getProperty("fcrepo.dynamic.test.port", "8080");

        System.setProperty("fcrepo.baseUrl", "http://localhost:" + webPort + "/fcrepo/rest");
        System.setProperty("fcrepo.authUsername", FEDORA_AUTH_USERNAME);
        System.setProperty("fcrepo.authPassword", FEDORA_AUTH_PASSWORD);
        System.setProperty("jms.brokerUrl", "tcp://localhost:" + jmsPort);
    }

    @Before
    public void setup() {
        final String webPort = System.getProperty("fcrepo.dynamic.test.port", "8080");
        final String basePath = "http://localhost:" + webPort + "/fcrepo/rest";
        final String subPath = post(basePath);

        for (int i = 0; i < 10; ++i) {
            post(basePath);
            post(subPath);
        }
    }

    @DirtiesContext
    @Test
    public void testReindexingRouter() throws Exception {
        final String webPort = System.getProperty("fcrepo.dynamic.test.port", "8080");

        resultEndpoint.expectedMinimumMessageCount(21);

        template.send("direct:reindex", new Processor() {
            @Override
            public void process(final Exchange exchange) throws Exception {
                exchange.getIn().setHeader(REINDEXING_RECIPIENTS, "mock:result");
                exchange.getIn().setHeader(FCREPO_URI, "http://localhost:" + webPort + "/fcrepo/rest/");
                exchange.getIn().setHeader(HTTP_URI, "http://localhost:" +  webPort + "/reindexing/");
                exchange.getIn().setHeader(REINDEXING_PREFIX, "/reindexing");
            }
        });

        MockEndpoint.assertIsSatisfied(resultEndpoint);
    }

    private String post(final String url) {
        final CloseableHttpClient httpclient = HttpClients.custom().setDefaultCredentialsProvider(provider).build();

        try {
            final HttpPost httppost = new HttpPost(url);

            final HttpResponse response = httpclient.execute(httppost);
            return EntityUtils.toString(response.getEntity(), "UTF-8");
        } catch (final IOException ex) {
            LOGGER.debug("Unable to extract HttpEntity response into an InputStream: ", ex);
            return "";
        }
    }

    @Configuration
    @ComponentScan(basePackages = {"org.fcrepo.camel"})
    static class ContextConfig extends CamelConfiguration {

        @Bean
        public RouteBuilder route() {
            return new ReindexingRouter();
        }
    }
}
