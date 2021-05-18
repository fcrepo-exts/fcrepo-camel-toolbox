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
package org.fcrepo.camel.service;

import static org.apache.http.HttpStatus.SC_CREATED;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_BASE_URL;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_IDENTIFIER;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.EndpointInject;
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

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;

/**
 * Test the route workflow.
 *
 * @author Aaron Coburn
 * @since 2016-07-21
 */
public class RouteIT extends CamelBlueprintTestSupport {

    private static final Logger LOGGER = getLogger(RouteIT.class);

    private static String FEDORA_USERNAME = "fedoraAdmin";
    private static String FEDORA_PASSWORD = "fedoraAdmin";

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    @Override
    protected String getBlueprintDescriptor() {
        return "/OSGI-INF/blueprint/blueprint-test.xml";
    }

    @Override
    protected void addServicesOnStartup(final Map<String, KeyValueHolder<Object, Dictionary>> services) {
        final String fcrepoPort = System.getProperty("fcrepo.dynamic.test.port", "8080");
        final FcrepoComponent component = new FcrepoComponent();

        component.setBaseUrl("http://localhost:" + fcrepoPort + "/fcrepo/rest");
        component.setAuthUsername(FEDORA_USERNAME);
        component.setAuthPassword(FEDORA_PASSWORD);
        services.put("fcrepo", asService(component, "osgi.jndi.service.name", "fcrepo/Camel"));
    }

    @Override
    public boolean isUseRouteBuilder() {
        return false;
    }

    @Test
    @Ignore("fix me")
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
        assertMockEndpointsSatisfied();
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
