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

import static org.apache.camel.Exchange.HTTP_METHOD;
import static org.fcrepo.camel.integration.FedoraTestUtils.getFcrepoBaseUrl;
import static org.fcrepo.camel.integration.FedoraTestUtils.getTurtleDocument;
import static org.fcrepo.camel.FedoraEndpoint.FCREPO_IDENTIFIER;

import org.apache.camel.Produce;
import org.apache.camel.Exchange;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

import java.util.Map;
import java.util.HashMap;
import java.io.IOException;

import org.springframework.test.context.ContextConfiguration;

/**
 * Test the fcr:transform endpoint
 * @author Aaron Coburn
 * @since November 7, 2014
 */
@ContextConfiguration({"/spring-test/test-container.xml"})
public class FedoraTransformIT extends CamelTestSupport {

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    @Test
    public void testTransform() throws IOException, InterruptedException {

        // Setup
        final Map<String, Object> headers = new HashMap<>();
        headers.put(Exchange.HTTP_METHOD, "POST");
        headers.put(Exchange.CONTENT_TYPE, "text/turtle");

        final String fullPath = template.requestBodyAndHeaders(
                "direct:setup", getTurtleDocument(), headers, String.class);

        final String identifier = fullPath.replaceAll(getFcrepoBaseUrl(), "");

        // Test
        template.sendBodyAndHeader(null, FCREPO_IDENTIFIER,
                identifier);

        // Teardown
        final Map<String, Object> teardownHeaders = new HashMap<>();
        teardownHeaders.put(HTTP_METHOD, "DELETE");
        teardownHeaders.put(FCREPO_IDENTIFIER, identifier);
        template.sendBodyAndHeaders("direct:teardown", null, teardownHeaders);

        // Assertions
        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.expectedHeaderReceived("Content-Type", "application/json");
        resultEndpoint.assertIsSatisfied();
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() throws IOException {
                final String fcrepo_uri = FedoraTestUtils.getFcrepoEndpointUri();

                from("direct:setup")
                    .to(fcrepo_uri);

                from("direct:start")
                    .to(fcrepo_uri + "?accept=application/json&transform=default")
                    .to("mock:result");

                from("direct:teardown")
                    .to(fcrepo_uri)
                    .to(fcrepo_uri + "?tombstone=true");
            }
        };
    }
}
