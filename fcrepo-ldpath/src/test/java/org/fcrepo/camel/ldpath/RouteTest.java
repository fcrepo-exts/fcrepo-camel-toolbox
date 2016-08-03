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
package org.fcrepo.camel.ldpath;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.Exchange.HTTP_URI;
import static org.apache.camel.util.ObjectHelper.loadResourceAsStream;
import static org.fcrepo.camel.ldpath.LDPathRouter.FEDORA_URI;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;

import org.junit.Test;

/**
 * Test the route workflow.
 *
 * @author acoburn
 * @since 2015-05-22
 */
public class RouteTest extends CamelBlueprintTestSupport {

    private final ObjectMapper MAPPER = new ObjectMapper();

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    @Override
    public boolean isUseAdviceWith() {
        return true;
    }

    @Override
    public boolean isUseRouteBuilder() {
        return false;
    }

    @Override
    protected String getBlueprintDescriptor() {
        return "/OSGI-INF/blueprint/blueprint-test.xml";
    }

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        final String restPort = System.getProperty("fcrepo.dynamic.ldpath.port", "9085");
        final String cacheDir = System.getProperty("project.build.directory", "target") + "/ldcache";

        final Properties props = new Properties();
        props.put("rest.port", restPort);
        props.put("cache.dir", cacheDir);
        return props;
    }

    @Test
    public void testGetDefault() throws Exception {
        final String uri = "http://purl.org/dc/terms/contributor";
        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.expectedHeaderReceived(CONTENT_TYPE, "application/json");

        context.getRouteDefinition("FcrepoLDPathGet").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveAddLast().to("mock:result");
            }
        });
        context.start();

        template.sendBodyAndHeader("direct:get", null, FEDORA_URI, uri);

        assertMockEndpointsSatisfied();
        final String result = resultEndpoint.getExchanges().get(0).getIn().getBody(String.class);

        @SuppressWarnings("unchecked")
        final List<Map<String, List<String>>> data = MAPPER.readValue(result, List.class);

        assertFalse(data.isEmpty());
        assertTrue(data.get(0).containsKey("label"));
        assertTrue(data.get(0).containsKey("type"));
        assertTrue(data.get(0).get("label").contains("Contributor"));
        assertTrue(data.get(0).get("type").contains("http://www.w3.org/1999/02/22-rdf-syntax-ns#Property"));
        assertTrue(data.get(0).get("id").contains(uri));
    }

    @Test
    public void testGetParam() throws Exception {
        final String uri = "http://fedora.info/definitions/v4/repository#Binary";
        getMockEndpoint("mock:http4:localhost").expectedMessageCount(1);
        getMockEndpoint("mock:http4:localhost").expectedHeaderReceived(HTTP_URI, "http://example.org/ldpath");

        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.expectedHeaderReceived(CONTENT_TYPE, "application/json");

        context.getRouteDefinition("FcrepoLDPathGet").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                mockEndpointsAndSkip("http4:*");
                weaveAddLast().to("mock:result");
            }
        });
        context.start();

        final Map<String, Object> headers = new HashMap<>();
        headers.put("ldpath", "http://example.org/ldpath");
        headers.put(FEDORA_URI, uri);
        template.sendBodyAndHeaders("direct:get", loadResourceAsStream("test.ldpath"), headers);

        assertMockEndpointsSatisfied();
        final String result = resultEndpoint.getExchanges().get(0).getIn().getBody(String.class);

        @SuppressWarnings("unchecked")
        final List<Map<String, List<String>>> data = MAPPER.readValue(result, List.class);

        assertFalse(data.isEmpty());
        assertTrue(data.get(0).containsKey("label"));
        assertTrue(data.get(0).containsKey("type"));
        assertTrue(data.get(0).get("label").contains("binary"));
        assertTrue(data.get(0).get("type").contains("Class"));
        assertTrue(data.get(0).get("id").contains(uri));
    }

    @Test
    public void testMimicPost() throws Exception {
        final String uri = "http://fedora.info/definitions/v4/repository#Container";
        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.expectedHeaderReceived(CONTENT_TYPE, "application/json");

        context.getRouteDefinition("FcrepoLDPathPrepare").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveAddLast().to("mock:result");
            }
        });
        context.start();

        template.sendBodyAndHeader("direct:ldpathPrepare", loadResourceAsStream("test.ldpath"), FEDORA_URI, uri);

        assertMockEndpointsSatisfied();
        final String result = resultEndpoint.getExchanges().get(0).getIn().getBody(String.class);

        @SuppressWarnings("unchecked")
        final List<Map<String, List<String>>> data = MAPPER.readValue(result, List.class);

        assertFalse(data.isEmpty());
        assertTrue(data.get(0).containsKey("label"));
        assertTrue(data.get(0).containsKey("type"));
        assertTrue(data.get(0).get("label").contains("Fedora Container"));
        assertTrue(data.get(0).get("type").contains("Class"));
        assertTrue(data.get(0).get("id").contains(uri));

    }
}
