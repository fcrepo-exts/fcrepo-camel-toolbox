/**
 * Copyright 2015 DuraSpace, Inc.
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
package org.fcrepo.camel.serialization;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.camel.util.ObjectHelper;
import org.apache.commons.io.IOUtils;

import static org.fcrepo.camel.JmsHeaders.EVENT_TYPE;
import static org.fcrepo.camel.RdfNamespaces.REPOSITORY;
import org.fcrepo.camel.FcrepoHeaders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Test the route workflow (property 'includeBinaries' is false).
 *
 * @author Bethany Seeger
 * @since 2015-09-28
 */

public class RouteTest extends CamelBlueprintTestSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(RouteTest.class);

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    private static final String baseURL = "http://localhost/rest";
    private static final String identifier = "/file1";
    private static final String auditContainer = "/audit";

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

        props.put("serialization.stream", "seda:foo");
        props.put("input.stream", "seda:bar");
        props.put("serialization.format", "RDF_XML");
        props.put("serialization.descriptions", "mock:direct:metadata_file");
        props.put("serialization.binaries", "mock:direct:binary_file");
        // in here to clearly show that we won't include binaries by default
        props.put("serialization.includeBinaries", "false");
        props.put("audit.container", auditContainer);

        return props;
    }

    @Test
    public void testMetatdataSerialization() throws Exception {
        context.getRouteDefinition("FcrepoSerialization").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpointsAndSkip("*");  // this mocks and skips sending to the ORIGINAL end point
            }
        });
        context.start();
        getMockEndpoint("mock:direct:metadata").expectedMessageCount(1);
        getMockEndpoint("mock:direct:binary").expectedMessageCount(1);
        getMockEndpoint("mock:direct:delete.metadata").expectedMessageCount(0);
        getMockEndpoint("mock:direct:delete.binary").expectedMessageCount(0);

        // send a file!
        final String body = IOUtils.toString(ObjectHelper.loadResourceAsStream("binary.rdf"), "UTF-8");
        template.sendBodyAndHeaders(body, createEvent());

        assertMockEndpointsSatisfied();
    }

    @Test
       public void testMetatdataSerializationRemoveNode() throws Exception {
        context.getRouteDefinition("FcrepoSerialization").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                //mockEndpointsAndSkip("fcrepo:*");
                mockEndpointsAndSkip("*");  // this mocks and skips sending to the ORIGINAL end point
            }
        });
        context.start();

        getMockEndpoint("mock:direct:metadata").expectedMessageCount(0);
        getMockEndpoint("mock:direct:binary").expectedMessageCount(0);
        getMockEndpoint("mock:direct:delete.metadata").expectedMessageCount(1);
        getMockEndpoint("mock:direct:delete.binary").expectedMessageCount(1);

        // send a file!
        final String body = IOUtils.toString(ObjectHelper.loadResourceAsStream("binary.rdf"), "UTF-8");
        template.sendBodyAndHeaders(body, createRemoveEvent());

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testMetadataReSerialization() throws Exception {
        context.getRouteDefinition("FcrepoReSerialization").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                //mockEndpointsAndSkip("fcrepo:*");
                mockEndpointsAndSkip("*");  // this mocks and skips sending to the ORIGINAL end point
            }
        });
        context.start();

        getMockEndpoint("mock:direct:metadata").expectedMessageCount(1);
        getMockEndpoint("mock:direct:binary").expectedMessageCount(1);

        // send a file!
        final String body = IOUtils.toString(ObjectHelper.loadResourceAsStream("binary.rdf"), "UTF-8");
        template.sendBodyAndHeaders(body, createEvent());

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testMetadataUpdaterIndexable() throws Exception {
        context.getRouteDefinition("FcrepoSerializationMetadataUpdater").adviceWith(context,
              new AdviceWithRouteBuilder() {
                  @Override
                  public void configure() throws Exception {
                      replaceFromWith("direct:start");
                      //mockEndpointsAndSkip("fcrepo:*");
                      mockEndpointsAndSkip("*");  // this mocks and skips sending to the ORIGINAL end point
                      weaveAddLast().to("mock:result");
                  }
              });
        context.start();

        resultEndpoint.expectedMessageCount(1);

        // send a file!
        final String body = IOUtils.toString(ObjectHelper.loadResourceAsStream("indexable.rdf"), "UTF-8");
        template.sendBodyAndHeaders(body, createEvent());

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testMetadataUpdaterBinary() throws Exception {
        context.getRouteDefinition("FcrepoSerializationBinaryUpdater").adviceWith(context,
          new AdviceWithRouteBuilder() {
              @Override
              public void configure() throws Exception {
                  replaceFromWith("direct:start");
                  mockEndpointsAndSkip("*");
              }
        });
        context.start();
        // this should be zero because writing binaries is disabled by default.
        getMockEndpoint("mock:direct:binary_file").expectedMessageCount(0);

        // send a file!
        final String body = IOUtils.toString(ObjectHelper.loadResourceAsStream("binary.rdf"), "UTF-8");
        template.sendBodyAndHeaders(body, createEvent("foo"));

        assertMockEndpointsSatisfied();
    }

    private static Map<String,Object> createEvent() {
        return createEvent(null);
    }

    private static Map<String,Object> createEvent(final String name) {

        final Map<String, Object> headers = new HashMap<>();
        headers.put(FcrepoHeaders.FCREPO_BASE_URL, baseURL);
        if (name != null && !name.isEmpty()) {
            headers.put(FcrepoHeaders.FCREPO_IDENTIFIER, name);
        }
        return headers;
    }

    private static Map<String,Object> createRemoveEvent() {

        final Map<String, Object> headers = new HashMap<>();
        headers.put(FcrepoHeaders.FCREPO_BASE_URL, baseURL);
        headers.put(FcrepoHeaders.FCREPO_IDENTIFIER, identifier);
        headers.put(EVENT_TYPE, REPOSITORY + "NODE_REMOVED");
        return headers;
    }
}
