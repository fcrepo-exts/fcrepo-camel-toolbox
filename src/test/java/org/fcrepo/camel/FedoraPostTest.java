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

public class FedoraPostTest extends CamelTestSupport {

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    @Test
    public void testContentTypeNew() throws Exception {
        final String body = "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n\n<> dc:title \"some title\" .";
        Map<String, Object> headers = new HashMap<String, Object>();
        headers.put(Exchange.HTTP_METHOD, "POST");
        headers.put(Exchange.CONTENT_TYPE, "text/turtle");
        
        template.sendBodyAndHeaders(body, headers);

        resultEndpoint.expectedMessageCount(1);
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

                from("direct:start")
                    .to("fcrepo:" + fcrepo_url)
                    .to("direct:setGetHeaders")
                    .to("fcrepo:" + fcrepo_url + "?contentType=application/rdf+xml")
                    .filter().xpath("/rdf:RDF/rdf:Description/rdf:type[@rdf:resource='http://fedora.info/definitions/v4/rest-api#resource']", ns)
                    .to("mock:result")
                    .to("direct:setDeleteHeaders")
                    .to("fcrepo:" + fcrepo_url);

                from("direct:setDeleteHeaders")
                    .setHeader(Exchange.HTTP_METHOD).constant("DELETE");

                from("direct:setGetHeaders")
                    .setHeader("FCREPO_IDENTIFIER").simple("${body.replaceAll(\"http://" + fcrepo_url + "\", \"\")}")
                    .setHeader(Exchange.HTTP_METHOD).constant("GET");
            }
        };
    }
}
