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

import org.apache.camel.Produce;
import org.apache.camel.Exchange;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

import java.util.Properties;
import java.util.Map;
import java.util.HashMap;

import java.io.InputStream;
import java.io.IOException;

public class FedoraFileTest extends CamelTestSupport {

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @EndpointInject(uri = "mock:file")
    protected MockEndpoint fileEndpoint;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    @Test
    public void testFile() throws Exception {
        // Assertions
        fileEndpoint.expectedBodiesReceived(FedoraTestUtils.getTextDocument());
        fileEndpoint.expectedMessageCount(1);
        fileEndpoint.expectedHeaderReceived("Content-Type", "text/plain");

        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.expectedHeaderReceived("Content-Type", "application/rdf+xml");

        Map<String, Object> headers = new HashMap<String, Object>();
        headers.put(Exchange.HTTP_METHOD, "POST");
        headers.put(Exchange.CONTENT_TYPE, "text/turtle");
        
        final String fullPath = template.requestBodyAndHeaders(
                "direct:setup",
                FedoraTestUtils.getTurtleDocument(),
                headers, String.class);

        // Strip off the baseUri to get the resource path
        final String identifier = fullPath.replaceAll(FedoraTestUtils.getFcrepoBaseUri(), "");

        Map<String, Object> fileHeaders = new HashMap<String, Object>();
        fileHeaders.put(Exchange.HTTP_METHOD, "PUT");
        fileHeaders.put(Exchange.CONTENT_TYPE, "text/plain");
        fileHeaders.put("FCREPO_IDENTIFIER", identifier + "/file");
        template.sendBodyAndHeaders("direct:setup", FedoraTestUtils.getTextDocument(), fileHeaders);

        template.sendBodyAndHeader(null, "FCREPO_IDENTIFIER", identifier + "/file");
        template.sendBodyAndHeader("direct:file", null, "FCREPO_IDENTIFIER", identifier + "/file");


        Map<String, Object> teardownHeaders = new HashMap<String, Object>();
        teardownHeaders.put(Exchange.HTTP_METHOD, "DELETE");
        teardownHeaders.put("FCREPO_IDENTIFIER", identifier + "/file");
        template.sendBodyAndHeaders("direct:teardown", null, teardownHeaders);
        teardownHeaders.put("FCREPO_IDENTIFIER", identifier);
        template.sendBodyAndHeaders("direct:teardown", null, teardownHeaders);

        // Confirm that assertions passed
        resultEndpoint.assertIsSatisfied();
        fileEndpoint.assertIsSatisfied();
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() throws IOException {

                final String fcrepo_uri = FedoraTestUtils.getFcrepoEndpointUri();

                Namespaces ns = new Namespaces("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");

                from("direct:setup")
                    .to(fcrepo_uri);
                
                from("direct:start")
                    .to(fcrepo_uri)
                    .filter().xpath("/rdf:RDF/rdf:Description/rdf:type[@rdf:resource='http://fedora.info/definitions/v4/rest-api#NonRdfSourceDescription']", ns)
                    .to("mock:result");

                from("direct:file")
                    .to(fcrepo_uri + "?metadata=false")
                    .to("mock:file");

                from("direct:teardown")
                    .to(fcrepo_uri)
                    .log("TEST TEARDOWN: ${header.FCREPO_IDENTIFIER}")
                    .to(fcrepo_uri + "?tombstone=true");
            }
        };
    }
}
