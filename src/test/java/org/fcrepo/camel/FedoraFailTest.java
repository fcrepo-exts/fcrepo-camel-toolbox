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
package org.fcrepo.camel;

import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.Exchange.HTTP_METHOD;
import static org.fcrepo.camel.FedoraEndpoint.FCREPO_IDENTIFIER;
import static org.fcrepo.camel.integration.FedoraTestUtils.getFcrepoBaseUrl;
import static org.fcrepo.camel.integration.FedoraTestUtils.getFcrepoEndpointUri;
import static org.fcrepo.camel.integration.FedoraTestUtils.getPatchDocument;
import static org.fcrepo.camel.integration.FedoraTestUtils.getTurtleDocument;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.XPathBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.http4.HttpOperationFailedException;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Test updating a fedora object with sparql-update
 * @author Aaron Coburn
 * @since November 7, 2014
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/spring-test/test-container.xml"})
public class FedoraFailTest extends CamelTestSupport {

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @EndpointInject(uri = "mock:rescued")
    protected MockEndpoint rescuedEndpoint;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    @Test
    public void testFail() throws InterruptedException {
        // Assertions
        resultEndpoint.expectedMessageCount(7);
        rescuedEndpoint.expectedMessageCount(7);

        // Setup
        final Map<String, Object> headers = new HashMap<>();
        headers.put(HTTP_METHOD, "POST");
        headers.put(CONTENT_TYPE, "text/turtle");

        final String fullPath = template.requestBodyAndHeaders(
                "direct:setup", getTurtleDocument(), headers, String.class);

        final String identifier = fullPath.replaceAll(getFcrepoBaseUrl(), "");

        // Test
        headers.clear();
        headers.put(HTTP_METHOD, "PATCH");
        headers.put(CONTENT_TYPE, "text/foo");
        headers.put(FCREPO_IDENTIFIER, identifier);
        template.sendBodyAndHeaders(getPatchDocument() + " Garbage ;; ", headers);
        template.sendBodyAndHeaders("direct:rescued", getPatchDocument() + " Garbage ;; ", headers);

        headers.clear();
        headers.put(HTTP_METHOD, "POST");
        headers.put(CONTENT_TYPE, "text/turtle");
        template.sendBodyAndHeaders("FOO -- ;;", headers);
        template.sendBodyAndHeaders("direct:rescued", "FOO -- ;;", headers);

        headers.clear();
        headers.put(HTTP_METHOD, "HEAD");
        headers.put(FCREPO_IDENTIFIER, "/foo/bar");
        template.sendBodyAndHeaders(null, headers);
        template.sendBodyAndHeaders("direct:rescued", null, headers);

        headers.clear();
        headers.put(HTTP_METHOD, "GET");
        headers.put(FCREPO_IDENTIFIER, "/foo/bar/baz");
        template.sendBodyAndHeaders(null, headers);
        template.sendBodyAndHeaders("direct:rescued", null, headers);

        headers.clear();
        headers.put(HTTP_METHOD, "PUT");
        headers.put(CONTENT_TYPE, "text/turtle");
        headers.put(FCREPO_IDENTIFIER, identifier + "/b/c");
        template.sendBodyAndHeaders("Garbage -- " + getPatchDocument() + " Garbage ;; ", headers);
        template.sendBodyAndHeaders("direct:rescued", "Garbage -- " + getPatchDocument() + " Garbage ;; ", headers);

        headers.clear();
        headers.put(HTTP_METHOD, "DELETE");
        headers.put(FCREPO_IDENTIFIER, "/foo/bar/baz/foo");
        template.sendBodyAndHeaders(null, headers);
        template.sendBodyAndHeaders("direct:rescued", null, headers);

        // Delete
        headers.clear();
        headers.put(HTTP_METHOD, "DELETE");
        headers.put(FCREPO_IDENTIFIER, identifier);
        template.sendBodyAndHeaders(null, headers);

        headers.clear();
        headers.put(HTTP_METHOD, "GET");
        headers.put(FCREPO_IDENTIFIER, identifier);
        template.sendBodyAndHeaders(null, headers);
        template.sendBodyAndHeaders("direct:rescued", null, headers);

        // Teardown
        final Map<String, Object> teardownHeaders = new HashMap<>();
        teardownHeaders.put(HTTP_METHOD, "DELETE");
        teardownHeaders.put(FCREPO_IDENTIFIER, identifier);
        template.sendBodyAndHeaders("direct:teardown", null, teardownHeaders);

        // Confirm that the assertions passed
        resultEndpoint.assertIsSatisfied();
        rescuedEndpoint.assertIsSatisfied();
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {

                final String fcrepo_uri = getFcrepoEndpointUri();

                final XPathBuilder xpath = new XPathBuilder("/rdf:RDF/rdf:Description/dc:title/text()");
                xpath.namespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
                xpath.namespace("dc", "http://purl.org/dc/elements/1.1/");

                onException(HttpOperationFailedException.class)
                    .handled(true)
                    .log("Error: ${headers}")
                    .to("mock:result");

                from("direct:setup")
                    .to(fcrepo_uri);

                from("direct:start")
                    .to(fcrepo_uri);

                from("direct:rescued")
                    .to(fcrepo_uri + "?throwExceptionOnFailure=false")
                    .to("mock:rescued");

                from("direct:teardown")
                    .to(fcrepo_uri + "?tombstone=true");
            }
        };
    }
}
