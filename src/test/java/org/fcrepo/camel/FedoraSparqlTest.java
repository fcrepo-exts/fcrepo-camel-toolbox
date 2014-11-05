/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/license/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software     
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.camel;

import org.fcrepo.camel.processor.SparqlUpdateProcessor;

import org.apache.camel.Produce;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.builder.xml.XPathBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

import java.util.Map;
import java.util.HashMap;

import java.io.IOException;

public class FedoraSparqlTest extends CamelTestSupport {

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    @Test
    public void testSparql() throws Exception {
        // Assertions
        resultEndpoint.expectedMessageCount(1);

        // Setup
        Map<String, Object> headers = new HashMap<String, Object>();
        headers.put(Exchange.HTTP_METHOD, "POST");
        headers.put(Exchange.CONTENT_TYPE, "text/turtle");

        final String fullPath = template.requestBodyAndHeaders(
                "direct:setup", FedoraTestUtils.getTurtleDocument(), headers, String.class);

        final String identifier = fullPath.replaceAll(FedoraTestUtils.getFcrepoBaseUri(), "");
        
        // Test
        Map<String, Object> testHeaders = new HashMap<String, Object>();
        testHeaders.put("FCREPO_IDENTIFIER", identifier);
        testHeaders.put("org.fcrepo.jms.baseURL", "http://localhost:8080/fcrepo4/rest");
        template.sendBodyAndHeaders(null, testHeaders);

        // Teardown
        Map<String, Object> teardownHeaders = new HashMap<String, Object>();
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

                final Processor sparqlUpdate = new SparqlUpdateProcessor();

                Namespaces ns = new Namespaces("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
                
                XPathBuilder titleXpath = new XPathBuilder("/rdf:RDF/rdf:Description/dc:title/text()");
                titleXpath.namespaces(ns);
                titleXpath.namespace("dc", "http://purl.org/dc/elements/1.1/");

                from("direct:setup")
                    .to(fcrepo_uri);

                from("direct:start")
                    .to(fcrepo_uri + "?accept=application/n-triples")
                    .process(sparqlUpdate)
                    .log("${body}")
                    .setHeader(Exchange.CONTENT_TYPE).constant("application/sparql-update")
//                    .to("http4:localhost:3030/test/update")
                    .to("mock:result");

                from("direct:teardown")
                    .to(fcrepo_uri)
                    .to(fcrepo_uri + "?tombstone=true");
            }
        };
    }
}
