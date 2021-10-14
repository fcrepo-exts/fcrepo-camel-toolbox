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
package org.fcrepo.camel.ldpath;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.spring.javaconfig.CamelConfiguration;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.Exchange.HTTP_METHOD;
import static org.apache.camel.Exchange.HTTP_URI;
import static org.apache.camel.util.ObjectHelper.loadResourceAsStream;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test the route workflow.
 *
 * @author acoburn
 * @since Aug 6, 2016
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {RouteTest.ContextConfig.class}, loader = AnnotationConfigContextLoader.class)
public class RouteTest {

    private final ObjectMapper MAPPER = new ObjectMapper();

    @EndpointInject("mock:language:simple:resource:classpath:org/fcrepo/camel/ldpath/options.ttl")
    protected MockEndpoint optionsEndpoint;

    @EndpointInject("mock:http:localhost")
    protected MockEndpoint httpEndpoint;

    @Produce("direct:start")
    protected ProducerTemplate template;

    @Autowired
    private CamelContext camelContext;

    @BeforeClass
    public static void beforeClass() {
        final String targetDir = System.getProperty("project.build.directory", "target");
        final var ldCacheDir = targetDir + File.separator + RouteTest.class.getCanonicalName() +
                File.separator + "ldcache";
        new File(ldCacheDir).mkdirs();
        System.setProperty("ldcache.directory", ldCacheDir);
        final String restPort = System.getProperty("fcrepo.dynamic.ldpath.port", "9085");
        System.setProperty("ldpath.rest.port", restPort);
        System.setProperty("ldpath.rest.host", "127.0.0.1");
    }

    @AfterClass
    public static void afterClass() {
        FileUtils.deleteQuietly(new File(System.getProperty("ldcache.directory")));
    }

    @Test
    public void testGetDefault() throws Exception {
        final String uri = "http://fedora.info/definitions/v4/event#ResourceCreation";
        final String endpoint = "mock:resultGet";
        final MockEndpoint mockEndpoint = (MockEndpoint) camelContext.getEndpoint(endpoint);
        mockEndpoint.expectedMessageCount(1);
        mockEndpoint.expectedHeaderReceived(CONTENT_TYPE, "application/json");
        final var context = camelContext.adapt(ModelCamelContext.class);
        AdviceWith.adviceWith(context, "FcrepoLDPathGet", a -> {
            a.weaveAddLast().to(endpoint);
        });

        template.sendBodyAndHeader("direct:get", null, "context", uri);

        mockEndpoint.assertIsSatisfied();
        final String result = mockEndpoint.getExchanges().get(0).getIn().getBody(String.class);

        @SuppressWarnings("unchecked")
        final List<Map<String, List<String>>> data = MAPPER.readValue(result, List.class);

        assertFalse(data.isEmpty());
        assertTrue(data.get(0).containsKey("label"));
        assertTrue(data.get(0).containsKey("type_ss"));
        assertTrue(data.get(0).get("id").contains(uri));
        assertTrue(data.get(0).get("label").contains("resource creation"));
        assertTrue(data.get(0).get("type_ss").contains("http://www.w3.org/2000/01/rdf-schema#Class"));
    }

    @Test
    public void testOptions() throws Exception {
        optionsEndpoint.expectedMessageCount(1);
        final var context = camelContext.adapt(ModelCamelContext.class);

        AdviceWith.adviceWith(context, "FcrepoLDPathRest", a -> {
            a.replaceFromWith("direct:start");
            a.mockEndpointsAndSkip("language:simple:resource:classpath:org/fcrepo/camel/ldpath/options.ttl");
        });

        template.sendBodyAndHeader(null, HTTP_METHOD, "OPTIONS");
        optionsEndpoint.assertIsSatisfied();
    }

    @Test
    public void testGetParam() throws Exception {
        final String uri = "http://fedora.info/definitions/v4/repository#Binary";
        final String endpoint = "mock:resultGetParam";
        final MockEndpoint mockEndpoint = (MockEndpoint) camelContext.getEndpoint(endpoint);
        httpEndpoint.expectedMessageCount(1);
        httpEndpoint.expectedHeaderReceived(HTTP_URI, "http://example.org/ldpath");
        final var context = camelContext.adapt(ModelCamelContext.class);
        mockEndpoint.expectedMessageCount(1);
        mockEndpoint.expectedHeaderReceived(CONTENT_TYPE, "application/json");

        AdviceWith.adviceWith(context, "FcrepoLDPathGet", a -> {
            a.mockEndpointsAndSkip("http:*");
            a.weaveAddLast().to(endpoint);
        });

        final Map<String, Object> headers = new HashMap<>();
        headers.put("ldpath", "http://example.org/ldpath");
        headers.put("context", uri);
        template.sendBodyAndHeaders("direct:get", loadResourceAsStream("test.ldpath"), headers);

        httpEndpoint.assertIsSatisfied();
        mockEndpoint.assertIsSatisfied();
        final String result = mockEndpoint.getExchanges().get(0).getIn().getBody(String.class);

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
        final String endpoint = "mock:resultPost";
        final MockEndpoint mockEndpoint = (MockEndpoint) camelContext.getEndpoint(endpoint);
        mockEndpoint.expectedMessageCount(1);
        mockEndpoint.expectedHeaderReceived(CONTENT_TYPE, "application/json");
        final var context = camelContext.adapt(ModelCamelContext.class);

        AdviceWith.adviceWith(context, "FcrepoLDPathPrepare", a -> {
            a.weaveAddLast().to(endpoint);
        });


        context.start();

        template.sendBodyAndHeader("direct:ldpathPrepare", loadResourceAsStream("test.ldpath"), "context", uri);

        mockEndpoint.assertIsSatisfied();
        final String result = mockEndpoint.getExchanges().get(0).getIn().getBody(String.class);

        @SuppressWarnings("unchecked")
        final List<Map<String, List<String>>> data = MAPPER.readValue(result, List.class);

        assertFalse(data.isEmpty());
        assertTrue(data.get(0).containsKey("label"));
        assertTrue(data.get(0).containsKey("type"));
        assertTrue(data.get(0).get("label").contains("Fedora Container"));
        assertTrue(data.get(0).get("type").contains("Class"));
        assertTrue(data.get(0).get("id").contains(uri));
    }


    @Configuration
    @ComponentScan(resourcePattern = "**/Fcrepo*.class")
    static class ContextConfig extends CamelConfiguration {
    }
}
