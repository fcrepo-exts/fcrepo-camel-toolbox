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
import static java.lang.System.getProperty;

import java.util.Map;
import java.util.HashMap;
import java.io.IOException;

import org.fcrepo.camel.processor.SparqlInsertProcessor;
import org.fcrepo.camel.processor.SparqlDescribeProcessor;
import org.fcrepo.camel.processor.SparqlDeleteProcessor;

import org.apache.camel.Produce;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.XPathBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Represents an integration test for interacting with an external triplestore.
 *
 * @author Aaron Coburn
 * @since Nov 8, 2014
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/spring-test/test-container.xml"})
public class FedoraSparqlIT extends CamelTestSupport {

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    @Test
    public void testSparql() throws Exception {
        // Assertions
        resultEndpoint.expectedMessageCount(2);

        // Setup
        final Map<String, Object> headers = new HashMap<String, Object>();
        headers.put(HTTP_METHOD, "POST");
        headers.put(CONTENT_TYPE, "text/turtle");

        final String fullPath = template.requestBodyAndHeaders(
                "direct:setup", FedoraTestUtils.getTurtleDocument(), headers, String.class);

        final String identifier = fullPath.replaceAll(FedoraTestUtils.getFcrepoBaseUrl(), "");

        // Test
        final Map<String, Object> testHeaders = new HashMap<String, Object>();
        testHeaders.put("FCREPO_IDENTIFIER", identifier);
        testHeaders.put("org.fcrepo.jms.baseURL", "http://localhost:8080/fcrepo4/rest");
        template.sendBodyAndHeaders(null, testHeaders);

        testHeaders.clear();
        testHeaders.put("org.fcrepo.jms.identifier", identifier);
        testHeaders.put("FCREPO_BASE_URL", "http://localhost:8080/fcrepo4/rest");
        template.sendBodyAndHeaders(null, testHeaders);

        // Teardown
        final Map<String, Object> teardownHeaders = new HashMap<String, Object>();
        teardownHeaders.put(Exchange.HTTP_METHOD, "DELETE");
        teardownHeaders.put("FCREPO_IDENTIFIER", identifier);
        template.sendBodyAndHeaders("direct:teardown", null, teardownHeaders);

        // Confirm that the assertions passed
        resultEndpoint.assertIsSatisfied();
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() throws IOException {

                final String fcrepo_uri = FedoraTestUtils.getFcrepoEndpointUri();
                final String fuseki_url = "localhost:" + getProperty("test.fuseki.port", "3030");
                final Processor sparqlInsert = new SparqlInsertProcessor();
                final XPathBuilder titleXpath = new XPathBuilder("/rdf:RDF/rdf:Description/dc:title/text()");
                titleXpath.namespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
                titleXpath.namespace("dc", "http://purl.org/dc/elements/1.1/");

                from("direct:setup")
                    .to(fcrepo_uri);

                from("direct:start")
                    .process(new SparqlDescribeProcessor())
                    .to("http4:" + fuseki_url + "/test/query")
                    //.log("${body}")
                    .process(new SparqlDeleteProcessor())
                    .to("http4:" + fuseki_url + "/test/update")
                    .setHeader(Exchange.HTTP_METHOD).constant("GET")
                    .to(fcrepo_uri + "?accept=application/n-triples")
                    .process(new SparqlInsertProcessor())
                    .to("http4:" + fuseki_url + "/test/update")
                    .to("mock:result");

                from("direct:teardown")
                    .to(fcrepo_uri)
                    .to(fcrepo_uri + "?tombstone=true");
            }
        };
    }
}
