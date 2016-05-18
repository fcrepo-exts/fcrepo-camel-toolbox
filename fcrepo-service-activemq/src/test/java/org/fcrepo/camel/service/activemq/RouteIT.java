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
package org.fcrepo.camel.service.activemq;

import static org.apache.http.HttpStatus.SC_CREATED;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Map;

import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.EndpointInject;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.camel.util.KeyValueHolder;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import org.junit.Test;
import org.slf4j.Logger;

/**
 * Test the route workflow.
 *
 * @author Aaron Coburn
 * @since 2016-05-04
 */
public class RouteIT extends CamelBlueprintTestSupport {

    private static final Logger LOGGER = getLogger(RouteIT.class);

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @Override
    protected String getBlueprintDescriptor() {
        return "/OSGI-INF/blueprint/blueprint-test.xml";
    }

    @Override
    protected void addServicesOnStartup(final Map<String, KeyValueHolder<Object, Dictionary>> services) {
        final String jmsPort = System.getProperty("fcrepo.dynamic.jms.port", "61616");
        final ActiveMQComponent component = new ActiveMQComponent();

        component.setBrokerURL("tcp://localhost:" + jmsPort);
        component.setExposeAllQueues(true);

        services.put("broker", asService(component, "osgi.jndi.service.name", "fcrepoqueue"));
    }

    @Override
    public boolean isUseRouteBuilder() {
        return false;
    }

    @Test
    public void testQueuingService() throws Exception {
        final String webPort = System.getProperty("fcrepo.dynamic.test.port", "8080");

        final String baseUrl = "http://localhost:" + webPort + "/fcrepo/rest";
        final String url1 = post(baseUrl).replace(baseUrl, "");
        final String url2 = post(baseUrl).replace(baseUrl, "");

        resultEndpoint.expectedMinimumMessageCount(2);
        resultEndpoint.expectedHeaderValuesReceivedInAnyOrder("org.fcrepo.jms.identifier", url1, url2);

        assertMockEndpointsSatisfied();
        final String jmsPort = System.getProperty("fcrepo.dynamic.jms.port", "61616");
        final String stompPort = System.getProperty("fcrepo.dynamic.stomp.port", "61613");
    }

    private String post(final String url) {
        final CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
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
