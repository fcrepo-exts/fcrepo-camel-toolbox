/**
 * Copyright 2014 DuraSpace, Inc.
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
package org.fcrepo.camel.integration;

import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.Exchange.HTTP_METHOD;
import static org.fcrepo.camel.integration.FedoraTestUtils.getFcrepoBaseUrl;
import static org.fcrepo.camel.integration.FedoraTestUtils.getFcrepoEndpointUri;
import static org.fcrepo.camel.integration.FedoraTestUtils.getPatchDocument;
import static org.fcrepo.camel.integration.FedoraTestUtils.getTurtleDocument;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.XPathBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

/**
 * Test updating a fedora object with sparql-update
 * @author Aaron Coburn
 * @since November 7, 2014
 */
@ContextConfiguration({"/spring-test/test-container.xml"})
public class FedoraPatchIT extends CamelTestSupport {

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @EndpointInject(uri = "mock:titles")
    protected MockEndpoint titleEndpoint;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    @Test
    public void testPatch() throws IOException, InterruptedException {
        // Assertions
        resultEndpoint.expectedMessageCount(1);
        titleEndpoint.expectedMessageCount(2);
        titleEndpoint.expectedBodiesReceivedInAnyOrder("some title", "another title");

        // Setup
        final Map<String, Object> headers = new HashMap<>();
        headers.put(HTTP_METHOD, "POST");
        headers.put(CONTENT_TYPE, "text/turtle");

        final String fullPath = template.requestBodyAndHeaders(
                "direct:setup", getTurtleDocument(), headers, String.class);

        final String identifier = fullPath.replaceAll(getFcrepoBaseUrl(), "");

        // Test
        final Map<String, Object> patchHeaders = new HashMap<>();
        patchHeaders.put(HTTP_METHOD, "PATCH");
        patchHeaders.put(CONTENT_TYPE, "text/turtle");
        patchHeaders.put("FCREPO_IDENTIFIER", identifier);

        template.sendBodyAndHeaders(getPatchDocument(), patchHeaders);

        template.sendBodyAndHeader("direct:test", null,
                "FCREPO_IDENTIFIER", identifier);

        // Teardown
        final Map<String, Object> teardownHeaders = new HashMap<>();
        teardownHeaders.put(HTTP_METHOD, "DELETE");
        teardownHeaders.put("FCREPO_IDENTIFIER", identifier);
        template.sendBodyAndHeaders("direct:teardown", null, teardownHeaders);

        // Confirm that the assertions passed
        resultEndpoint.assertIsSatisfied();
        titleEndpoint.assertIsSatisfied();
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() throws IOException {

                final String fcrepo_uri = getFcrepoEndpointUri();

                final XPathBuilder xpath = new XPathBuilder("/rdf:RDF/rdf:Description/dc:title/text()");
                xpath.namespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
                xpath.namespace("dc", "http://purl.org/dc/elements/1.1/");

                from("direct:setup")
                    .to(fcrepo_uri);

                from("direct:start")
                    .to(fcrepo_uri)
                    .to("mock:result");

                from("direct:test")
                    .to(fcrepo_uri)
                    .split(xpath)
                    .to("mock:titles");

                from("direct:teardown")
                    .to(fcrepo_uri)
                    .to(fcrepo_uri + "?tombstone=true");
            }
        };
    }
}
