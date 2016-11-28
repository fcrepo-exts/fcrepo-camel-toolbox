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
package org.fcrepo.camel.serialization;

import static org.apache.camel.Exchange.FILE_NAME;
import static org.apache.camel.util.ObjectHelper.loadResourceAsStream;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_IDENTIFIER;

import org.junit.Test;
import java.util.Properties;

import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.commons.io.IOUtils;

/**
 * Test the route workflow with the 'includeBinaries' config property set to true.
 *
 * @author Bethany Seeger
 * @since 2015-11-24
 */

public class BinaryEnabledRouteTest extends AbstractRouteTest {

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        final Properties props = new Properties();

        props.put("serialization.stream", "seda:foo");
        props.put("input.stream", "seda:bar");
        props.put("serialization.format", "RDF_XML");
        props.put("serialization.descriptions", "metadata_file");
        props.put("serialization.binaries", "binary_file");
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

        getMockEndpoint("mock:file:binary_file").expectedMessageCount(1);
        getMockEndpoint("mock:file:binary_file").expectedHeaderReceived(FILE_NAME, "/foo");

        // send a file!
        final String body = IOUtils.toString(loadResourceAsStream("binary.rdf"), "UTF-8");

        template.sendBodyAndHeader(body, FCREPO_IDENTIFIER, "/foo");

        assertMockEndpointsSatisfied();
    }
}
