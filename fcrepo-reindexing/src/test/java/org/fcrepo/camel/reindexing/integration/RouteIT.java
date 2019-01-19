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

import static org.apache.camel.Exchange.HTTP_URI;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.fcrepo.camel.reindexing.ReindexingHeaders.REINDEXING_RECIPIENTS;
import static org.fcrepo.camel.reindexing.ReindexingHeaders.REINDEXING_PREFIX;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Map;
import java.util.Properties;

import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.camel.util.KeyValueHolder;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.fcrepo.camel.FcrepoComponent;

import org.junit.Test;
import org.slf4j.Logger;

/**
 * Test the route workflow.
 *
 * @author Aaron Coburn
 * @since 2015-04-10
 */
public class RouteIT extends CamelBlueprintTestSupport {

    private static final Logger LOGGER = getLogger(RouteIT.class);

    private static final String FEDORA_AUTH_USERNAME = "fedoraAdmin";
    private static final String FEDORA_AUTH_PASSWORD = "fedoraAdmin";

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce(uri = "direct:reindex")
    protected ProducerTemplate template;

    private String fullPath = "";

    @Override
    protected String getBlueprintDescriptor() {
        return "/OSGI-INF/blueprint/blueprint-test.xml";
    }

    @Override
    public void doPreSetup() throws Exception {
        super.doPreSetup();
        final String webPort = System.getProperty("fcrepo.dynamic.test.port", "8080");

        final String basePath = "http://localhost:" + webPort + "/fcrepo/rest";
        final String subPath = post(basePath);
        final String jmsPort = System.getProperty("fcrepo.dynamic.jms.port", "61616");

        for (int i = 0; i < 10; ++i) {
            post(basePath);
            post(subPath);
        }
    }
    @Override
    protected void addServicesOnStartup(final Map<String, KeyValueHolder<Object, Dictionary>> services) {
        final String jmsPort = System.getProperty("fcrepo.dynamic.jms.port", "61616");
        final String webPort = System.getProperty("fcrepo.dynamic.test.port", "8080");
        final ActiveMQComponent amq = new ActiveMQComponent();

        amq.setBrokerURL("tcp://localhost:" + jmsPort);
        amq.setExposeAllQueues(true);
        final FcrepoComponent fcrepo = new FcrepoComponent();
        fcrepo.setBaseUrl("http://localhost:" + webPort + "/fcrepo/rest");
        fcrepo.setAuthUsername(FEDORA_AUTH_USERNAME);
        fcrepo.setAuthPassword(FEDORA_AUTH_PASSWORD);
        services.put("broker", asService(amq, "osgi.jndi.service.name", "fcrepo/Broker"));
        services.put("fcrepo", asService(fcrepo, "osgi.jndi.service.name", "fcrepo/Camel"));
    }

    @Override
    public boolean isUseRouteBuilder() {
        return false;
    }

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        final String restPort = System.getProperty("fcrepo.dynamic.reindexing.port", "9080");

        final Properties props = new Properties();
        props.put("reindexing.stream", "broker:queue:reindexing");
        props.put("rest.prefix", "/reindexing");
        props.put("rest.port", restPort);

        return props;
    }

    @Test
    public void testReindexingRouter() throws Exception {
        final String webPort = System.getProperty("fcrepo.dynamic.test.port", "8080");

        getMockEndpoint("mock:result").expectedMinimumMessageCount(21);

        template.send("direct:reindex", new Processor() {
            public void process(final Exchange exchange) throws Exception {
                exchange.getIn().setHeader(REINDEXING_RECIPIENTS, "mock:result");
                exchange.getIn().setHeader(FCREPO_URI, "http://localhost:" + webPort + "/fcrepo/rest/");
                exchange.getIn().setHeader(HTTP_URI, "http://localhost:9080/reindexing/");
                exchange.getIn().setHeader(REINDEXING_PREFIX, "/reindexing");
            }
        });

        assertMockEndpointsSatisfied();
    }

    private String post(final String url) {
        final BasicCredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(FEDORA_AUTH_USERNAME, FEDORA_AUTH_PASSWORD));
        final CloseableHttpClient httpclient = HttpClients.custom().setDefaultCredentialsProvider(provider).build();

        try {
            final HttpPost httppost = new HttpPost(url);

            final HttpResponse response = httpclient.execute(httppost);
            return EntityUtils.toString(response.getEntity(), "UTF-8");
        } catch (IOException ex) {
            LOGGER.debug("Unable to extract HttpEntity response into an InputStream: ", ex);
            return "";
        }
    }
}
