package org.fcrepo.camel;

import org.apache.camel.Produce;
import org.apache.camel.Exchange;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

import java.io.IOException;

public class FedoraContentTypeHeaderTest extends CamelTestSupport {

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    @Test
    public void testContentTypeJson() throws Exception {
        resultEndpoint.expectedHeaderReceived("Content-Type", "application/ld+json");
        resultEndpoint.expectedMessageCount(2);

        template.sendBodyAndHeader(null, "Accept", "application/ld+json");
        template.sendBodyAndHeader(null, Exchange.ACCEPT_CONTENT_TYPE, "application/ld+json");

        resultEndpoint.assertIsSatisfied();
    }

    @Test
    public void testContentTypeRdfXml() throws Exception {
        resultEndpoint.expectedHeaderReceived("Content-Type", "application/rdf+xml");
        resultEndpoint.expectedMessageCount(2);

        template.sendBodyAndHeader(null, "Accept", "application/rdf+xml");
        template.sendBodyAndHeader(null, Exchange.ACCEPT_CONTENT_TYPE, "application/rdf+xml");

        resultEndpoint.assertIsSatisfied();
    }
    
    @Test
    public void testContentTypeNTriples() throws Exception {
        resultEndpoint.expectedHeaderReceived("Content-Type", "application/n-triples");
        resultEndpoint.expectedMessageCount(2);

        template.sendBodyAndHeader(null, "Accept", "application/n-triples");
        template.sendBodyAndHeader(null, Exchange.ACCEPT_CONTENT_TYPE, "application/n-triples");

        resultEndpoint.assertIsSatisfied();
    }

    @Test
    public void testContentTypeTurtle() throws Exception {
        resultEndpoint.expectedHeaderReceived("Content-Type", "text/turtle");
        resultEndpoint.expectedMessageCount(2);

        template.sendBodyAndHeader(null, "Accept", "text/turtle");
        template.sendBodyAndHeader(null, Exchange.ACCEPT_CONTENT_TYPE, "text/turtle");

        resultEndpoint.assertIsSatisfied();
    }

    @Test
    public void testContentTypeN3() throws Exception {
        resultEndpoint.expectedHeaderReceived("Content-Type", "text/rdf+n3");
        resultEndpoint.expectedMessageCount(2);

        template.sendBodyAndHeader(null, "Accept", "text/rdf+n3");
        template.sendBodyAndHeader(null, Exchange.ACCEPT_CONTENT_TYPE, "text/rdf+n3");

        resultEndpoint.assertIsSatisfied();
    }


    @Test
    public void testContentTypeDefault() throws Exception {
        resultEndpoint.expectedHeaderReceived("Content-Type", FedoraEndpoint.DEFAULT_CONTENT_TYPE);
        resultEndpoint.expectedMessageCount(1);

        template.sendBody(null);

        resultEndpoint.assertIsSatisfied();
    }


    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() throws IOException {
                final String fcrepo_uri = FedoraTestUtils.getFcrepoEndpointUri();

                from("direct:start")
                    .to(fcrepo_uri)
                    .to("mock:result");
            }
        };
    }
}
