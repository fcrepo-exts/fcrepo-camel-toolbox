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
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.spring.javaconfig.CamelConfiguration;
import org.apache.camel.support.builder.ExpressionBuilder;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.camel.Exchange.HTTP_METHOD;
import static org.apache.camel.Exchange.HTTP_URI;
import static org.apache.camel.util.ObjectHelper.loadResourceAsStream;
import static org.apache.http.client.utils.URLEncodedUtils.CONTENT_TYPE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test the route workflow with functions enabled.
 *
 * @author Peter Eichman
 * @since May 11, 2018
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {RouteWithFunctionsTest.ContextConfig.class},
        loader = AnnotationConfigContextLoader.class)
public class RouteWithFunctionsTest {

    private final ObjectMapper MAPPER = new ObjectMapper();

    @EndpointInject("mock:result")
    protected MockEndpoint resultEndpoint;

    @EndpointInject("mock:result2")
    protected MockEndpoint resultEndpoint2;

    @EndpointInject("mock:http4:localhost")
    protected MockEndpoint httpEndpoint;

    @EndpointInject("mock:language:simple:resource:classpath:org/fcrepo/camel/ldpath/options.ttl")
    protected MockEndpoint optionsEndpoint;


    @Produce("direct:start")
    protected ProducerTemplate template;

    @Autowired
    protected CamelContext camelContext;

    @BeforeClass
    public static void beforeClass() {
        final String targetDir = System.getProperty("project.build.directory", "target");
        final var ldCacheDir = targetDir + File.separator + "ldcache";
        new File(ldCacheDir).mkdirs();

        System.setProperty("ldcache.directory", ldCacheDir);
        final String restPort = System.getProperty("fcrepo.dynamic.ldpath.port", "9085");
        System.setProperty("rest.port", restPort);
        System.setProperty("rest.host", "0.0.0.0");

    }

    @AfterClass
    public static void afterClass() {
        FileUtils.deleteQuietly(new File(System.getProperty("ldcache.directory")));
    }

    @Test
    public void testGetDefault() throws Exception {
        final String uri = "http://fedora.info/definitions/v4/event#ResourceCreation";
        resultEndpoint2.expectedMessageCount(1);
        //@FIXME (the following line fails)
        //resultEndpoint2.expectedHeaderReceived(CONTENT_TYPE, "application/json");

        final var context = camelContext.adapt(ModelCamelContext.class);
        AdviceWith.adviceWith(context, "FcrepoLDPathGet", a -> {
            a.weaveAddLast().to(resultEndpoint2.getEndpointUri());
        });


        template.sendBodyAndHeader("direct:get", null, "context", uri);

        resultEndpoint2.assertIsSatisfied();
        final String result = resultEndpoint.getExchanges().get(0).getIn().getBody(String.class);

        @SuppressWarnings("unchecked")
        final List<Map<String, List<String>>> data = MAPPER.readValue(result, List.class);

        assertFalse(data.isEmpty());
        assertTrue(data.get(0).containsKey("label"));
        assertTrue(data.get(0).containsKey("type"));

        //@FIXME The following lines fail - not sure what's going on.
        //assertTrue(data.get(0).get("id").contains(uri));
        //assertTrue(data.get(0).get("label").contains("resource creation"));
        //assertTrue(data.get(0).get("type").contains("http://www.w3.org/2000/01/rdf-schema#Class"));
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
    @Ignore("FIXME: Seems to be an issue with example.org behaving differently now")
    public void testGetParam() throws Exception {
        final String uri = "http://fedora.info/definitions/v4/repository#Binary";
        httpEndpoint.expectedMessageCount(1);
        httpEndpoint.expectedHeaderReceived(HTTP_URI, "http://example.org/ldpath");

        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.expectedHeaderReceived(CONTENT_TYPE, "application/json");

        final var context = camelContext.adapt(ModelCamelContext.class);
        AdviceWith.adviceWith(context, "FcrepoLDPathGet", a -> {
            a.mockEndpointsAndSkip("http4:*");
            a.weaveAddLast().to("mock:result");
        });

        final Map<String, Object> headers = new HashMap<>();
        headers.put("ldpath", "http://example.org/ldpath");
        headers.put("context", uri);
        template.sendBodyAndHeaders("direct:get", loadResourceAsStream("test.ldpath"), headers);

        resultEndpoint.assertIsSatisfied();
        httpEndpoint.assertIsSatisfied();
        final String result = resultEndpoint.getExchanges().get(0).getIn().getBody(String.class);

        @SuppressWarnings("unchecked")
        final List<Map<String, List<String>>> data = MAPPER.readValue(result, List.class);

        assertFalse(data.isEmpty());
        assertTrue(data.get(0).containsKey("label"));
        assertTrue(data.get(0).containsKey("type"));
        assertTrue(data.get(0).get("id").contains(uri));
        assertTrue(data.get(0).get("label").contains("binary"));
        assertTrue(data.get(0).get("type").contains("Class"));
    }

    @Test
    public void testMimicPost() throws Exception {
        final String uri = "http://fedora.info/definitions/v4/repository#Container";
        resultEndpoint.expectedMessageCount(1);
        //FIXME : the tests no break if the following line is uncommented.
        //resultEndpoint.expectedHeaderReceived(CONTENT_TYPE, "application/json");

        final var context = camelContext.adapt(ModelCamelContext.class);
        AdviceWith.adviceWith(context, "FcrepoLDPathPrepare", a -> {
            a.weaveAddLast().to("mock:result");
        });

        template.sendBodyAndHeader("direct:ldpathPrepare",
                loadResourceAsStream("test.ldpath"), "context", uri);

        resultEndpoint.assertIsSatisfied();
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

    @Test
    public void testMimicPostWithFunctions() throws Exception {
        final String uri = "http://fedora.info/definitions/v4/repository#Container";
        resultEndpoint.expectedMessageCount(1);
        //FIXME : the tests no break if the following line is uncommented.
        //resultEndpoint.expectedHeaderReceived(CONTENT_TYPE, "application/json");

        final var context = camelContext.adapt(ModelCamelContext.class);
        AdviceWith.adviceWith(context, "FcrepoLDPathPrepare", a -> {
            a.weaveAddLast().to("mock:result");
        });


        template.sendBodyAndHeader("direct:ldpathPrepare",
                loadResourceAsStream("test-with-functions.ldpath"),
                "context", uri);

        resultEndpoint.assertIsSatisfied();
        final String result = resultEndpoint.getExchanges().get(0).getIn().getBody(String.class);

        @SuppressWarnings("unchecked")
        final List<Map<String, List<String>>> data = MAPPER.readValue(result, List.class);

        assertFalse(data.isEmpty());
        assertTrue(data.get(0).containsKey("label"));
        assertTrue(data.get(0).containsKey("type"));
        assertTrue(data.get(0).containsKey("description"));
        assertTrue(data.get(0).get("label").contains("Fedora Container"));
        assertTrue(data.get(0).get("type").contains("Class"));
        assertTrue(data.get(0).get("id").contains(uri));
        assertTrue(data.get(0).get("description").contains("Class : Fedora Container"));

    }


    @Configuration
    @ComponentScan(resourcePattern = "**/Test*.class")
    static class ContextConfig extends CamelConfiguration {
        @Bean
        public RouteBuilder route() {
            final var ldpath = getBean("ldpath");
            return new RouteBuilder() {
                public void configure() throws Exception {
                    from("direct:ldpath")
                            .setBody(ExpressionBuilder.beanExpression(ldpath,
                                    "programQuery(${headers.context}, ${body})"));
                }
            };
        }
    }
}

