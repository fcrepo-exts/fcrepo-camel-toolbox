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

import static org.fcrepo.camel.JmsHeaders.EVENT_TYPE;
import static org.fcrepo.camel.RdfNamespaces.REPOSITORY;
import org.fcrepo.camel.FcrepoHeaders;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.camel.util.ObjectHelper;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.junit.Test;

/**
 * Test the route workflow with the 'includeBinaries' config property set to true.
 *
 * @author Bethany Seeger
 * @since 2015-11-24
 */

public class RouteTestBinaryEnabled extends CamelBlueprintTestSupport {

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
        props.put("serialization.includeBinaries", "true");
        props.put("audit.container", auditContainer);

        return props;
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

        getMockEndpoint("mock:direct:binary_file").expectedMessageCount(1);
        getMockEndpoint("mock:direct:binary_file").expectedHeaderReceived(Exchange.FILE_NAME, "foo");

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
