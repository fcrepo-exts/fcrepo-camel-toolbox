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

import static org.fcrepo.camel.JmsHeaders.BASE_URL;
import static org.fcrepo.camel.JmsHeaders.IDENTIFIER;

import org.junit.Test;
import java.util.Map;
import java.util.Properties;

import com.google.common.collect.ImmutableMap;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.util.ObjectHelper;
import org.apache.commons.io.IOUtils;

/**
 * Test the route workflow with the 'includeBinaries' config property set to true.
 *
 * @author Bethany Seeger
 * @since 2015-11-24
 */

public class RouteTestBinaryEnabled extends AbstractRouteTest {

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
        final Map<String, Object> headers = ImmutableMap.of(
            BASE_URL, baseURL,
            IDENTIFIER, "foo");

        template.sendBodyAndHeaders(body, headers);

        assertMockEndpointsSatisfied();
    }
}
