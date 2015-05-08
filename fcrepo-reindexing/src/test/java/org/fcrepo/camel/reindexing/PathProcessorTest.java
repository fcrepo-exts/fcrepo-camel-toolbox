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
package org.fcrepo.camel.reindexing;

import java.io.IOException;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.fcrepo.camel.FcrepoHeaders;

import org.junit.Test;

/**
 * Test the route workflow.
 *
 * @author Aaron Coburn
 * @since 2015-05-8
 */
public class PathProcessorTest extends CamelTestSupport {

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    @Test
    public void testPathProcessor() throws Exception {

        final String path = "/foo/bar";
        final String root = "/";
        final String baseUrl1 = "http://localhost:8080/fcrepo/rest";
        final String baseUrl2 = "localhost:8983/fcrepo/rest";

        resultEndpoint.expectedMessageCount(4);
        resultEndpoint.expectedHeaderValuesReceivedInAnyOrder(
                FcrepoHeaders.FCREPO_IDENTIFIER, root, path, root, path);

        template.sendBodyAndHeader(baseUrl1 + path, FcrepoHeaders.FCREPO_BASE_URL, baseUrl1);
        template.sendBodyAndHeader(baseUrl1 + root, FcrepoHeaders.FCREPO_BASE_URL, baseUrl1);
        template.sendBodyAndHeader("http://" + baseUrl2 + path, FcrepoHeaders.FCREPO_BASE_URL, baseUrl2);
        template.sendBodyAndHeader("http://" + baseUrl2 + root, FcrepoHeaders.FCREPO_BASE_URL, baseUrl2);

        assertMockEndpointsSatisfied();
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() throws IOException {
                from("direct:start")
                    .process(new PathProcessor())
                    .to("mock:result");
            }
        };
    }
}
