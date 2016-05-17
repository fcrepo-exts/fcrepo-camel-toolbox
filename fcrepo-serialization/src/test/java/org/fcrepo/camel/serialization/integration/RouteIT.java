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
package org.fcrepo.camel.serialization.integration;

import static com.jayway.awaitility.Awaitility.await;
import static org.fcrepo.camel.RdfNamespaces.REPOSITORY;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.camel.util.ObjectHelper;
import org.fcrepo.camel.JmsHeaders;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoResponse;

import org.junit.Test;
import org.slf4j.Logger;

/**
 * Test the route workflow.
 *
 * @author bseeger
 * @since 2015-11-05
 */

public class RouteIT extends CamelBlueprintTestSupport {

    final private Logger logger = getLogger(RouteIT.class);

    private static final String FCREPO_PORT = System.getProperty(
            "fcrepo.dynamic.test.port", "8080");

    private static final String auditContainer = "/audit";

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    private String fullPath = "";
    private final String binary = "binary.txt";

    @Override
    protected void doPreSetup() throws Exception {
        final FcrepoClient client = new FcrepoClient(null, null, null, true);
        final FcrepoResponse res = client.post(
                URI.create("http://localhost:" + FCREPO_PORT + "/fcrepo/rest"),
                ObjectHelper.loadResourceAsStream("indexable.ttl"), "text/turtle");
        fullPath = res.getLocation().toString();
    }

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
        return "/OSGI-INF/blueprint/blueprint.xml";
    }

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {

        final Properties props = new Properties();
        props.put("fcrepo.baseUrl", "localhost:" + FCREPO_PORT + "/fcrepo/rest");
        props.put("serialization.descriptions", "target/serialization/descriptions");
        props.put("serialization.binaries", "target/serialization/binaries");
        props.put("serialization.stream", "direct:foo");
        props.put("input.stream", "direct:start");
        return props;
    }

    @Test
    public void testAddedEventRouter() throws Exception {
        final String path = fullPath.replaceFirst("http://localhost:[0-9]+/fcrepo/rest", "");
        final String fcrepoEndpoint = "mock:fcrepo:localhost:" + FCREPO_PORT + "/fcrepo/rest";

        context.getRouteDefinition("FcrepoSerialization").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                mockEndpoints("*");
            }
        });
        context.getRouteDefinition("FcrepoSerializationMetadataUpdater").adviceWith(context,
                new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                mockEndpoints("*");
            }
        });
        context.getRouteDefinition("FcrepoSerializationBinaryUpdater").adviceWith(context,
                new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                mockEndpoints("*");
            }
        });

        context.start();

        final Map<String, Object> headers = new HashMap<>();
        headers.put(JmsHeaders.IDENTIFIER, path);
        headers.put(JmsHeaders.BASE_URL, "http://localhost:" + FCREPO_PORT + "/fcrepo/rest");
        headers.put(JmsHeaders.EVENT_TYPE, REPOSITORY + "NODE_ADDED");
        headers.put(JmsHeaders.TIMESTAMP, 1428360320168L);
        headers.put(JmsHeaders.PROPERTIES, "");

        getMockEndpoint("mock://direct:metadata").expectedMessageCount(1);
        getMockEndpoint("mock://direct:binary").expectedMessageCount(1);
        // Binary request should not go through, so only 1 message to the fcrepoEndpoint
        getMockEndpoint(fcrepoEndpoint).expectedMessageCount(1);

        final File f = new File("target/serialization/descriptions/" + path  + ".ttl");

        assertFalse(f.exists());

        template.sendBodyAndHeaders("direct:start", "", headers);

        await().until(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return f.exists();
            }
        });

        assertMockEndpointsSatisfied();
    }
}
