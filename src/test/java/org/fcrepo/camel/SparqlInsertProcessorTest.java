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
import static org.apache.commons.lang3.StringUtils.normalizeSpace;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.ArrayUtils.reverse;
import static org.fcrepo.camel.integration.FedoraTestUtils.getFcrepoEndpointUri;
import static org.fcrepo.camel.integration.FedoraTestUtils.getN3Document;
import static org.fcrepo.camel.FedoraEndpoint.FCREPO_BASE_URL;
import static org.fcrepo.camel.FedoraEndpoint.FCREPO_IDENTIFIER;
import static org.fcrepo.jms.headers.DefaultMessageFactory.BASE_URL_HEADER_NAME;
import static org.fcrepo.jms.headers.DefaultMessageFactory.IDENTIFIER_HEADER_NAME;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.fcrepo.camel.processor.SparqlInsertProcessor;
import org.junit.Test;

/**
 * Test adding a non-RDF resource
 * @author Aaron Coburn
 * @since November 7, 2014
 */
public class SparqlInsertProcessorTest extends CamelTestSupport {

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    @Test
    public void testDescribe() throws IOException, InterruptedException {
        final String base = "http://localhost/rest";
        final String path = "/path/a/b/c/d";
        final String document = getN3Document();

        // Reverse the lines as the RDF is serialized in opposite order
        final String[] lines = document.split("\n");
        reverse(lines);

        // Assertions
        resultEndpoint.expectedBodiesReceived("INSERT DATA { " + join(lines, " ") + " }");
        resultEndpoint.expectedHeaderReceived("Content-Type", "application/sparql-update");
        resultEndpoint.expectedHeaderReceived(HTTP_METHOD, "POST");

        // Test
        final Map<String, Object> headers = new HashMap<>();
        headers.put(FCREPO_BASE_URL, base);
        headers.put(FCREPO_IDENTIFIER, path);
        template.sendBodyAndHeaders(document, headers);

        headers.clear();
        headers.put(BASE_URL_HEADER_NAME, base);
        headers.put(IDENTIFIER_HEADER_NAME, path);
        template.sendBodyAndHeaders(document, headers);

        headers.clear();
        headers.put(BASE_URL_HEADER_NAME, base);
        headers.put(FCREPO_IDENTIFIER, path);
        template.sendBodyAndHeaders(document, headers);

        headers.clear();
        headers.put(FCREPO_BASE_URL, base);
        headers.put(IDENTIFIER_HEADER_NAME, path);
        template.sendBodyAndHeaders(document, headers);

        // Confirm that assertions passed
        resultEndpoint.expectedMessageCount(4);
        resultEndpoint.assertIsSatisfied();
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() throws IOException {

                final String fcrepo_uri = getFcrepoEndpointUri();

                from("direct:start")
                    .process(new SparqlInsertProcessor())
                    // Normalize the whitespace to make it easier to compare
                    .process(new Processor() {
                        public void process(final Exchange exchange) throws Exception {
                           final String payload = exchange.getIn().getBody(String.class);
                           exchange.getIn().setBody(normalizeSpace(payload));
                       }
                    })
                    .to("mock:result");
            }
        };
    }
}
