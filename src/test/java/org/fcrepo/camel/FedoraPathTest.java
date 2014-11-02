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

public class FedoraPathTest extends CamelTestSupport {

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template1;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template2;

    @Produce(uri = "direct:start3")
    protected ProducerTemplate template3;

    @Produce(uri = "direct:setup")
    protected ProducerTemplate setup;

    @Produce(uri = "direct:teardown")
    protected ProducerTemplate teardown;

    @Test
    public void testPath() throws Exception {
        final String path = "/test/a/b/c/d";
        final String body = "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n\n<> dc:title \"some title\" .";

        Map<String, Object> setupHeaders = new HashMap<String, Object>();
        setupHeaders.put(Exchange.HTTP_METHOD, "PUT");
        setupHeaders.put("FCREPO_IDENTIFIER", path);
        setupHeaders.put(Exchange.CONTENT_TYPE, "text/turtle");
        setup.sendBodyAndHeaders(body, setupHeaders);
 
        template1.sendBodyAndHeader(null, "org.fcrepo.jms.identifier", path);
        template2.sendBodyAndHeader(null, "FCREPO_IDENTIFIER", path);
        template3.sendBody(null);

        Map<String, Object> teardownHeaders = new HashMap<String, Object>();
        teardownHeaders.put(Exchange.HTTP_METHOD, "DELETE");
        teardownHeaders.put("FCREPO_IDENTIFIER", path);
        teardown.sendBodyAndHeaders(null, teardownHeaders);

        resultEndpoint.expectedMessageCount(3);

        resultEndpoint.assertIsSatisfied();
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() throws IOException {
                Properties props = new Properties();

                InputStream in = getClass().getResourceAsStream("/org.fcrepo.properties");
                props.load(in);
                in.close();

                String fcrepo_url = props.getProperty("fcrepo.url").replaceAll("http://", "");

                Namespaces ns = new Namespaces("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");

                from("direct:setup")
                    .log("Setup: ${headers}")
                    .to("fcrepo:" + fcrepo_url);
                
                from("direct:start")
                    .log("Start: ${headers}")
                    .to("fcrepo:" + fcrepo_url)
                    .filter().xpath("/rdf:RDF/rdf:Description/rdf:type[@rdf:resource='http://fedora.info/definitions/v4/rest-api#resource']", ns)
                    .to("mock:result");

                from("direct:start3")
                    .to("fcrepo:" + fcrepo_url + "/test/a/b/c/d")
                    .filter().xpath("/rdf:RDF/rdf:Description/rdf:type[@rdf:resource='http://fedora.info/definitions/v4/rest-api#resource']", ns)
                    .to("mock:result");

                from("direct:teardown")
                    .to("fcrepo:" + fcrepo_url)
                    .to("fcrepo:" + fcrepo_url + "?tombstone=true");
            }
        };
    }
}
