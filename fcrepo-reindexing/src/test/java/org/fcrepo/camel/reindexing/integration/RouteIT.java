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
package org.fcrepo.camel.reindexing.integration;

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
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.fcrepo.camel.reindexing.ReindexingHeaders;
import org.fcrepo.camel.FcrepoHeaders;

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
        final ActiveMQComponent component = new ActiveMQComponent();

        component.setBrokerURL("tcp://localhost:" + jmsPort);
        component.setExposeAllQueues(true);

        services.put("broker", asService(component, null));
    }

    @Override
    public boolean isUseRouteBuilder() {
        return false;
    }

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        final String webPort = System.getProperty("fcrepo.dynamic.test.port", "8080");
        final String restPort = System.getProperty("fcrepo.dynamic.reindexing.port", "9080");

        final Properties props = new Properties();
        props.put("fcrepo.baseUrl", "localhost:" + webPort + "/fcrepo/rest");
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
                exchange.getIn().setHeader(ReindexingHeaders.RECIPIENTS, "mock:result");
                exchange.getIn().setHeader(FcrepoHeaders.FCREPO_BASE_URL,
                    "http://localhost:" + webPort + "/fcrepo/rest");
                exchange.getIn().setHeader(FcrepoHeaders.FCREPO_IDENTIFIER, "/");
                exchange.getIn().setHeader(Exchange.HTTP_URI, "http://localhost:9080/reindexing/");
                exchange.getIn().setHeader(ReindexingHeaders.REST_PREFIX, "/reindexing");
            }
        });

        assertMockEndpointsSatisfied();
    }

    private String post(final String url) {
        final CloseableHttpClient httpclient = HttpClients.createDefault();
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
