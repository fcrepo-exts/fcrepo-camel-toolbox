package org.fcrepo.camel;

import org.apache.camel.Produce;
import org.apache.camel.Exchange;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.XPathBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

import java.util.Properties;
import java.util.Map;
import java.util.HashMap;

import java.io.InputStream;
import java.io.IOException;

public class FedoraPatchTest extends CamelTestSupport {

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @EndpointInject(uri = "mock:titles")
    protected MockEndpoint titleEndpoint;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    @Test
    public void testPatch() throws Exception {
        // Assertions
        resultEndpoint.expectedMessageCount(1);
        titleEndpoint.expectedMessageCount(2);
        titleEndpoint.expectedBodiesReceivedInAnyOrder("some title",
                "another title");

        // Setup
        Map<String, Object> headers = new HashMap<String, Object>();
        headers.put(Exchange.HTTP_METHOD, "POST");
        headers.put(Exchange.CONTENT_TYPE, "text/turtle");

        final String fullPath = template.requestBodyAndHeaders(
                "direct:setup", FedoraTestUtils.getTurtleDocument(), headers, String.class);

        final String identifier = fullPath.replaceAll(FedoraTestUtils.getFcrepoBaseUri(), "");
        
        // Test
        Map<String, Object> patchHeaders = new HashMap<String,
            Object>();
        patchHeaders.put(Exchange.HTTP_METHOD, "PATCH");
        patchHeaders.put(Exchange.CONTENT_TYPE, "text/turtle");
        patchHeaders.put("FCREPO_IDENTIFIER", identifier);
            
        template.sendBodyAndHeaders(FedoraTestUtils.getPatchDocument(), patchHeaders);

        template.sendBodyAndHeader("direct:test", null,
                "FCREPO_IDENTIFIER", identifier);

        // Teardown
        Map<String, Object> teardownHeaders = new HashMap<String, Object>();
        teardownHeaders.put(Exchange.HTTP_METHOD, "DELETE");
        teardownHeaders.put("FCREPO_IDENTIFIER", identifier);
        template.sendBodyAndHeaders("direct:teardown", null, teardownHeaders);

        // Confirm that the assertions passed
        resultEndpoint.assertIsSatisfied();
        titleEndpoint.assertIsSatisfied();
    }
    
    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() throws IOException {

                final String fcrepo_uri = FedoraTestUtils.getFcrepoEndpointUri();

                XPathBuilder xpath = new XPathBuilder("/rdf:RDF/rdf:Description/dc:title/text()");
                xpath.namespace("rdf",
                        "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
                xpath.namespace("dc",
                        "http://purl.org/dc/elements/1.1/");

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
