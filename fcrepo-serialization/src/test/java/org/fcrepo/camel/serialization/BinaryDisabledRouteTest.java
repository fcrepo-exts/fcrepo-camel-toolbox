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

import static org.apache.camel.util.ObjectHelper.loadResourceAsStream;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_IDENTIFIER;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;

import org.junit.Test;

import org.apache.camel.builder.AdviceWithRouteBuilder;

/**
 * Test the route workflow (property 'includeBinaries' is false).
 *
 * @author Bethany Seeger
 * @since 2015-09-28
 */

public class BinaryDisabledRouteTest extends AbstractRouteTest {

    @Test
    public void testMetatdataSerialization() throws Exception {
        context.getRouteDefinition("FcrepoSerialization").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpointsAndSkip("*");
            }
        });
        context.start();

        getMockEndpoint("mock:direct:metadata").expectedMessageCount(1);
        getMockEndpoint("mock:direct:binary").expectedMessageCount(1);
        getMockEndpoint("mock:direct:delete").expectedMessageCount(0);

        // send a file!
        template.sendBody(loadResourceAsStream("event.json"));

        assertMockEndpointsSatisfied();
    }

    @Test
       public void testMetadataSerializationRemoveNode() throws Exception {
        context.getRouteDefinition("FcrepoSerialization").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpointsAndSkip("*");
            }
        });
        context.start();

        getMockEndpoint("mock:direct:metadata").expectedMessageCount(0);
        getMockEndpoint("mock:direct:binary").expectedMessageCount(0);
        getMockEndpoint("mock:direct:delete").expectedMessageCount(1);

        template.sendBody(loadResourceAsStream("event_delete_resource.json"));

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testMetadataReSerialization() throws Exception {
        context.getRouteDefinition("FcrepoReSerialization").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpointsAndSkip("*");
            }
        });
        context.start();

        getMockEndpoint("mock:direct:metadata").expectedMessageCount(1);
        getMockEndpoint("mock:direct:binary").expectedMessageCount(1);

        template.sendBodyAndHeader(loadResourceAsStream("binary.rdf"), FCREPO_URI, baseURL);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testMetadataUpdaterIndexable() throws Exception {
        context.getRouteDefinition("FcrepoSerializationMetadataUpdater").adviceWith(context,
              new AdviceWithRouteBuilder() {
                  @Override
                  public void configure() throws Exception {
                      replaceFromWith("direct:start");
                      mockEndpointsAndSkip("*");
                      weaveAddLast().to("mock:result");
                  }
              });
        context.start();

        resultEndpoint.expectedMessageCount(1);

        // send a file!
        template.sendBody(loadResourceAsStream("indexable.rdf"));

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
        getMockEndpoint("mock:file:binary_file").expectedMessageCount(0);

        template.sendBodyAndHeader(loadResourceAsStream("binary.rdf"), FCREPO_IDENTIFIER, "/foo");

        assertMockEndpointsSatisfied();
    }
}
