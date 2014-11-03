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

        template.sendBodyAndHeader("direct:teardown",
                null, "FCREPO_IDENTIFIER", identifier + "/file");
        template.sendBodyAndHeader("direct:teardown",
                null, "FCREPO_IDENTIFIER", identifier);

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
                    .filter().xpath("/rdf:RDF/rdf:Description/rdf:type[@rdf:resource='http://fedora.info/definitions/v4/rest-api#datastream']", ns)
                    .to("mock:result");

                from("direct:file")
                    .to(fcrepo_uri + "?metadata=false")
                    .to("mock:file");

                from("direct:teardown")
                    .to(fcrepo_uri)
                    .to(fcrepo_uri + "?tombstone=true");
            }
        };
    }
}
