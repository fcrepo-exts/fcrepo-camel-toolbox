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
package org.fcrepo.camel.ldpath.integration;

import static org.apache.camel.util.ObjectHelper.loadResourceAsStream;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.InputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

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

    private final ObjectMapper MAPPER = new ObjectMapper();

    private final CloseableHttpClient httpclient = HttpClients.createDefault();

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

        final String basePath = "http://localhost:" + webPort + "/fcrepo/rest/testing";
        put(basePath, loadResourceAsStream("container.ttl"), "text/turtle");
        put(basePath + "/child");
    }

    @Override
    public boolean isUseRouteBuilder() {
        return false;
    }

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        final String webPort = System.getProperty("fcrepo.dynamic.test.port", "8080");
        final String ldpathPort = System.getProperty("fcrepo.dynamic.ldpath.port", "9082");
        final String cacheDir = System.getProperty("project.build.directory", "target") + "/ldcache2";

        final Properties props = new Properties();
        props.put("fcrepo.baseUrl", "http://localhost:" + webPort + "/fcrepo/rest");
        props.put("cache.dir", cacheDir);
        props.put("rest.port", ldpathPort);

        return props;
    }

    @Test
    public void testDefaultGet() throws Exception {
        final String ldpathPort = System.getProperty("fcrepo.dynamic.ldpath.port", "9085");
        final String webPort = System.getProperty("fcrepo.dynamic.test.port", "8080");

        String response = get("http://localhost:" + ldpathPort + "/ldpath/testing");

        @SuppressWarnings("unchecked")
        final List<Map<String, List<String>>> data1 = MAPPER.readValue(response, List.class);

        assertFalse(data1.isEmpty());
        assertTrue(data1.get(0).containsKey("label"));
        assertTrue(data1.get(0).containsKey("type"));
        assertTrue(data1.get(0).containsKey("id"));
        assertTrue(data1.get(0).get("label").contains("Some Object"));
        assertTrue(data1.get(0).get("type").contains("http://pcdm.org/models#Object"));
        assertTrue(data1.get(0).get("id").contains("http://localhost:" + webPort +
                    "/fcrepo/rest/testing"));

        response = get("http://localhost:" + ldpathPort + "/ldpath/testing/child");

        @SuppressWarnings("unchecked")
        final List<Map<String, List<String>>> data2 = MAPPER.readValue(response, List.class);

        assertFalse(data2.isEmpty());
        assertTrue(data2.get(0).containsKey("hasParent"));
        assertTrue(data2.get(0).containsKey("type"));
        assertTrue(data2.get(0).containsKey("id"));
        assertTrue(data2.get(0).get("hasParent").contains("http://localhost:" + webPort + "/fcrepo/rest/testing"));
        assertTrue(data2.get(0).get("type").contains("http://www.w3.org/ns/ldp#Container"));
        assertTrue(data2.get(0).get("id").contains("http://localhost:" + webPort +
                    "/fcrepo/rest/testing/child"));
    }

    @Test
    public void testPost() throws Exception {
        final String webPort = System.getProperty("fcrepo.dynamic.test.port", "9085");
        final String ldpathPort = System.getProperty("fcrepo.dynamic.ldpath.port", "9085");
        String response = post("http://localhost:" + ldpathPort + "/ldpath/testing",
                loadResourceAsStream("test.ldpath"), "application/ldpath");

        @SuppressWarnings("unchecked")
        final List<Map<String, List<String>>> data = MAPPER.readValue(response, List.class);

        assertFalse(data.isEmpty());
        assertTrue(data.get(0).containsKey("label"));
        assertTrue(data.get(0).containsKey("type"));
        assertTrue(data.get(0).containsKey("id"));
        assertTrue(data.get(0).get("label").contains("Some Object"));
        assertTrue(data.get(0).get("type").contains("Object"));
        assertTrue(data.get(0).get("type").contains("Fedora resource"));
        assertTrue(data.get(0).get("id").contains("http://localhost:" + webPort + "/fcrepo/rest/testing"));

        response = post("http://localhost:" + ldpathPort + "/ldpath/testing/child",
                loadResourceAsStream("test.ldpath"), "application/ldpath");

        @SuppressWarnings("unchecked")
        final List<Map<String, List<String>>> data2 = MAPPER.readValue(response, List.class);
        assertFalse(data2.isEmpty());
        assertTrue(data2.get(0).containsKey("id"));
        assertTrue(data2.get(0).containsKey("label"));
        assertTrue(data2.get(0).containsKey("type"));
        assertTrue(data2.get(0).containsKey("comment"));
        assertTrue(data2.get(0).get("comment").isEmpty());
        assertTrue(data2.get(0).get("id").contains("http://localhost:" + webPort + "/fcrepo/rest/testing/child"));
        assertTrue(data2.get(0).get("label").isEmpty());
        assertTrue(data2.get(0).get("type").contains("Fedora Container"));

    }

    @Test
    public void testOptions() throws Exception {
       final String ldpathPort = System.getProperty("fcrepo.dynamic.ldpath.port", "9085");
       final String response = options("http://localhost:" + ldpathPort + "/ldpath/testing");

       assertTrue(response.contains("rdfs:label \"LDPath Service\" ;"));
    }

    private String get(final String url) {
        try {
            final HttpResponse response = httpclient.execute(new HttpGet(url));
            return EntityUtils.toString(response.getEntity(), "UTF-8");
        } catch (IOException ex) {
            LOGGER.debug("Unable to extract HttpEntity response into an InputStream: ", ex);
            return "";
        }
    }

    private String post(final String url) {
        return post(url, null, null);
    }

    private String post(final String url, final InputStream entity, final String contentType) {
        try {
            final HttpPost req = new HttpPost(url);
            if (entity != null) {
                req.setHeader("Content-Type", contentType);
                req.setEntity(new InputStreamEntity(entity));
            }
            final HttpResponse response = httpclient.execute(req);
            return EntityUtils.toString(response.getEntity(), "UTF-8");
        } catch (IOException ex) {
            LOGGER.debug("Unable to extract HttpEntity response into an InputStream: ", ex);
            return "";
        }
    }

    private String put(final String url) {
        return put(url, null, null);
    }

    private String put(final String url, final InputStream entity, final String contentType) {
        try {
            final HttpPut req = new HttpPut(url);
            if (entity != null) {
                req.setHeader("Content-Type", contentType);
                req.setEntity(new InputStreamEntity(entity));
            }
            final HttpResponse response = httpclient.execute(req);
            return EntityUtils.toString(response.getEntity(), "UTF-8");
        } catch (IOException ex) {
            LOGGER.debug("Unable to extract HttpEntity response into an InputStream: ", ex);
            return "";
        }
    }

    private String options(final String url) {
        try {
            final HttpOptions req = new HttpOptions(url);
            final HttpResponse response = httpclient.execute(req);
            return EntityUtils.toString(response.getEntity(), "UTF-8");
        } catch (IOException ex) {
            LOGGER.debug("Unable to extract HttpEntity response into an InputStream: ", ex);
            return "";
        }
    }

}
