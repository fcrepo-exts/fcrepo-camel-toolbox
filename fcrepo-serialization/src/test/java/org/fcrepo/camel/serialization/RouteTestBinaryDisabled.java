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
import static org.fcrepo.camel.JmsHeaders.EVENT_TYPE;
import static org.fcrepo.camel.JmsHeaders.IDENTIFIER;
import static org.fcrepo.camel.RdfNamespaces.REPOSITORY;

import org.junit.Test;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.util.ObjectHelper;
import org.apache.commons.io.IOUtils;

/**
 * Test the route workflow (property 'includeBinaries' is false).
 *
 * @author Bethany Seeger
 * @since 2015-09-28
 */

public class RouteTestBinaryDisabled extends AbstractRouteTest {

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
        getMockEndpoint("mock:direct:delete.metadata").expectedMessageCount(0);
        getMockEndpoint("mock:direct:delete.binary").expectedMessageCount(0);

        // send a file!
        final String body = IOUtils.toString(ObjectHelper.loadResourceAsStream("binary.rdf"), "UTF-8");

        final Map<String, Object> headers = ImmutableMap.of(BASE_URL, baseURL);

        template.sendBodyAndHeaders(body, headers);
        assertMockEndpointsSatisfied();
    }

    @Test
       public void testMetatdataSerializationRemoveNode() throws Exception {
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
        getMockEndpoint("mock:direct:delete.metadata").expectedMessageCount(1);
        getMockEndpoint("mock:direct:delete.binary").expectedMessageCount(1);

        // send a file!
        final String body = IOUtils.toString(ObjectHelper.loadResourceAsStream("binary.rdf"), "UTF-8");
        final Map<String, Object> headers = ImmutableMap.of(
            BASE_URL, baseURL,
            IDENTIFIER, identifier,
            EVENT_TYPE, REPOSITORY + "NODE_REMOVE");

        template.sendBodyAndHeaders(body, headers);

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

        // send a file!
        final String body = IOUtils.toString(ObjectHelper.loadResourceAsStream("binary.rdf"), "UTF-8");
        final Map<String, Object> headers = ImmutableMap.of(BASE_URL, baseURL);

        template.sendBodyAndHeaders(body, headers);

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
        final String body = IOUtils.toString(ObjectHelper.loadResourceAsStream("indexable.rdf"), "UTF-8");
        final Map<String, Object> headers = ImmutableMap.of(BASE_URL, baseURL);

        template.sendBodyAndHeaders(body, headers);

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
        final Map<String, Object> headers = ImmutableMap.of(
            BASE_URL, baseURL,
            IDENTIFIER, "foo");

        template.sendBodyAndHeaders(body, headers);

        assertMockEndpointsSatisfied();
    }
}
