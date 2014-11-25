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

import static org.apache.camel.Exchange.HTTP_METHOD;
import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.fcrepo.camel.FedoraEndpoint.FCREPO_IDENTIFIER;
import static org.fcrepo.camel.integration.FedoraTestUtils.getFcrepoBaseUrl;
import static org.fcrepo.camel.integration.FedoraTestUtils.getFcrepoEndpointUri;
import static org.fcrepo.camel.integration.FedoraTestUtils.getTurtleDocument;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Test the fcr:transform endpoint
 * @author Aaron Coburn
 * @since November 7, 2014
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/spring-test/test-container.xml"})
public class FedoraTransformTest extends CamelTestSupport {

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    @Test
    public void testTransform() throws InterruptedException {

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

        final String ldpath = "@prefix fcrepo : <http://fedora.info/definitions/v4/repository#>\n" +
            "id      = . :: xsd:string ;\n" +
            "title = dc:title :: xsd:string;\n" +
            "uuid = fcrepo:uuid :: xsd:string;";
        headers.clear();
        headers.put(FCREPO_IDENTIFIER, identifier);
        headers.put(CONTENT_TYPE, "application/rdf+ldpath");
        headers.put(HTTP_METHOD, "POST");
        template.sendBodyAndHeaders("direct:post", ldpath, headers);

        headers.clear();
        headers.put(FCREPO_IDENTIFIER, identifier);
        headers.put(HTTP_METHOD, "GET");
        template.sendBodyAndHeaders("direct:get", null, headers);


        // Teardown
        final Map<String, Object> teardownHeaders = new HashMap<>();
        teardownHeaders.put(HTTP_METHOD, "DELETE");
        teardownHeaders.put(FCREPO_IDENTIFIER, identifier);
        template.sendBodyAndHeaders("direct:teardown", null, teardownHeaders);


        // Assertions
        resultEndpoint.expectedMessageCount(3);
        resultEndpoint.expectedHeaderReceived("Content-Type", "application/json");
        resultEndpoint.assertIsSatisfied();
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                final String fcrepo_uri = getFcrepoEndpointUri();

                from("direct:setup")
                    .to(fcrepo_uri);

                from("direct:start")
                    .to(fcrepo_uri + "?accept=application/foo&transform=default")
                    .to("mock:result");

                from("direct:get")
                    .to(fcrepo_uri + "?transform=default")
                    .to("mock:result");

                from("direct:post")
                    .to(fcrepo_uri + "?transform=true")
                    .to("mock:result");

                from("direct:teardown")
                    .to(fcrepo_uri)
                    .to(fcrepo_uri + "?tombstone=true");
            }
        };
    }
}
