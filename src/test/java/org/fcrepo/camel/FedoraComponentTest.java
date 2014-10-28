package org.fcrepo.camel;

import org.apache.camel.Processor;
import org.apache.camel.Message;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;
import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;


public class FedoraComponentTest extends CamelTestSupport {

    @Test
    public void testFedora() throws Exception {
        MockEndpoint mockJsonLd = getMockEndpoint("mock:jsonld");
        mockJsonLd.expectedMessageCount(3);
        mockJsonLd.expectedHeaderReceived("Content-Type", "application/ld+json");

        MockEndpoint mockXml = getMockEndpoint("mock:xml");
        mockXml.expectedMessageCount(1);
        mockXml.expectedHeaderReceived("Content-Type", "application/rdf+xml");
        
        MockEndpoint mockNonRdf = getMockEndpoint("mock:non-rdf");
        mockNonRdf.expectedHeaderReceived("Content-Type", "application/rdf+xml");
        mockNonRdf.expectedMessageCount(1);

        MockEndpoint mockResource = getMockEndpoint("mock:resource");
        mockResource.expectedHeaderReceived("Content-Type", "application/rdf+xml");
        mockResource.expectedMessageCount(1);

        MockEndpoint mockPut = getMockEndpoint("mock:put");
        mockPut.expectedMessageCount(1);

        MockEndpoint mockPatch = getMockEndpoint("mock:patch");
        mockPatch.expectedMessageCount(1);
            
        MockEndpoint mockPost = getMockEndpoint("mock:post");
        mockPost.expectedMessageCount(1);
            
        MockEndpoint mockDelete = getMockEndpoint("mock:delete");
        mockDelete.expectedMessageCount(2);

        assertMockEndpointsSatisfied();
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() throws IOException {

                Properties props = new Properties();
                InputStream in = getClass().getResourceAsStream("/org.fcrepo.properties");
                props.load(in);
                in.close();

                String fcrepo_url = props.getProperty("org.fcrepo.test.url").replaceAll("http://", "");

                Namespaces ns = new Namespaces("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");

                from("timer://foo?repeatCount=1")
                    .log("Using fcrepo endpoint at http://" + fcrepo_url);

                from("timer://bar?repeatCount=1")
                  .multicast().to("direct:resource", "direct:post", "direct:put")
                  .end();
                  
                from("direct:post")
                    .multicast().to("direct:post1");

                from("direct:put")
                    .multicast().to("direct:put1");

                from("direct:post1")
                    .setHeader("Exchange.HTTP_METHOD").constant("POST")
                    .setHeader("Exchange.CONTENT_TYPE").constant("text/turtle")
                    .process(new Processor() {
                        public void process(Exchange exchange) {
                            Message in = exchange.getIn();
                            in.setBody("PREFIX dc: <http://purl.org/dc/elements/1.1/>\n\n<> dc:title \"some title\" .");
                        }})
                    .to("fcrepo:" + fcrepo_url)
                    .to("mock:post")
                    .setHeader("Exchange.HTTP_METHOD").constant("DELETE")
                    .setHeader("FCREPO_IDENTIFIER").simple("${body.replaceAll(\"http://" + fcrepo_url + "\", \"\")}")
                    .to("fcrepo:" + fcrepo_url)
                    .to("mock:delete");

                from("direct:put1")
                    .setHeader("Exchange.HTTP_METHOD").constant("PUT")
                    .setHeader("Exchange.CONTENT_TYPE").constant("text/turtle")
                    .setHeader("FCREPO_IDENTIFIER").constant("/testing/object3")
                    .process(new Processor() {
                        public void process(Exchange exchange) {
                            Message in = exchange.getIn();
                            in.setBody("PREFIX dc: <http://purl.org/dc/elements/1.1/> \n\n<> dc:title \"some title\" .");
                        }})
                    .to("fcrepo:" + fcrepo_url)
                    .to("mock:put")
                    .setHeader("Exchange.HTTP_METHOD").constant("PATCH")
                    .process(new Processor() {
                        public void process(Exchange exchange) {
                            Message in = exchange.getIn();
                            in.setBody("PREFIX dc: <http://purl.org/dc/elements/1.1/> \n\nINSERT { <> dc:title \"some-resource-title\" . } \nWHERE { }");
                        }})
                    .to("fcrepo:" + fcrepo_url)
                    .to("mock:patch")
                    .setHeader("FCREPO_IDENTIFIER").simple("/testing/object3")
                    .setHeader("Exchange.HTTP_METHOD").constant("DELETE")
                    .to("fcrepo:" + fcrepo_url)
                    .setHeader("FCREPO_IDENTIFIER").simple("/testing/object3/fcr:tombstone")
                    .setHeader("Exchange.HTTP_METHOD").constant("DELETE")
                    .to("fcrepo:" + fcrepo_url)
                    .to("mock:delete");

                from("direct:resource")
                    .setHeader("Exchange.HTTP_METHOD").constant("POST")
                    .setHeader("Exchange.CONTENT_TYPE").constant("text/turtle")
                    .process(new Processor() {
                        public void process(Exchange exchange) {
                            Message in = exchange.getIn();
                            in.setBody("PREFIX dc: <http://purl.org/dc/elements/1.1/>\n\n<> dc:title \"some title\" .");
                        }})
                    .to("fcrepo:" + fcrepo_url)
                    .setHeader("org.fcrepo.jms.identifier").simple("${body.replaceAll(\"http://" + fcrepo_url + "\", \"\")}")
                    .setHeader("FCREPO_IDENTIFIER").simple("${header.org.fcrepo.jms.identifier}/file")
                    .setHeader("Exchange.HTTP_METHOD").constant("PUT")
                    .setHeader("Exchange.CONTENT_TYPE").constant("text/plain")
                    .process(new Processor() {
                        public void process(Exchange exchange) {
                            Message in = exchange.getIn();
                            in.setBody("This is a sample document");
                        }})
                    .to("fcrepo:" + fcrepo_url)
                    .multicast().to("direct:xml1", "direct:xml2", "direct:xml3", "direct:json1", "direct:json2", "direct:json3")
                    .setHeader("FCREPO_IDENTIFIER").simple("${header.org.fcrepo.jms.identifier}/file")
                    .setHeader("Exchange.HTTP_METHOD").constant("DELETE")
                    .to("fcrepo:" + fcrepo_url)
                    .setHeader("FCREPO_IDENTIFIER").simple("${header.org.fcrepo.jms.identifier}/file/fcr:tombstone")
                    .setHeader("Exchange.HTTP_METHOD").constant("DELETE")
                    .to("fcrepo:" + fcrepo_url)
                    .setHeader("FCREPO_IDENTIFIER").simple("${header.org.fcrepo.jms.identifier}")
                    .setHeader("Exchange.HTTP_METHOD").constant("DELETE")
                    .to("fcrepo:" + fcrepo_url)
                    .setHeader("FCREPO_IDENTIFIER").simple("${header.org.fcrepo.jms.identifier}/fcr:tombstone")
                    .setHeader("Exchange.HTTP_METHOD").constant("DELETE")
                    .to("fcrepo:" + fcrepo_url);

                from("direct:xml1")
                  .setHeader("Exchange.HTTP_METHOD").constant("GET")
                  .setHeader("Exchange.CONTENT_TYPE").constant("application/rdf+xml")
                  .to("fcrepo:" + fcrepo_url)
                  .to("mock:xml");
                    
                from("direct:xml2")
                  .setHeader("Exchange.HTTP_METHOD").constant("GET")
                  .setHeader("FCREPO_IDENTIFIER").simple("${header.org.fcrepo.jms.identifier}")
                  .to("fcrepo:" + fcrepo_url + "?contentType=application/rdf+xml")
                  .filter().xpath("/rdf:RDF/rdf:Description/rdf:type[@rdf:resource='http://fedora.info/definitions/v4/rest-api#resource']", ns)
                  .to("mock:resource");
                
                from("direct:xml3")
                  .setHeader("Exchange.HTTP_METHOD").constant("GET")
                  .setHeader("FCREPO_IDENTIFIER").simple("${header.org.fcrepo.jms.identifier}/file")
                  .setHeader("Exchange.CONTENT_TYPE").constant("application/rdf+xml")
                  .to("fcrepo:" + fcrepo_url)
                  .filter().xpath("/rdf:RDF/rdf:Description/rdf:type[@rdf:resource='http://fedora.info/definitions/v4/rest-api#datastream']", ns)
                  .to("mock:non-rdf");

                from("direct:json1")
                  .setHeader("Exchange.HTTP_METHOD").constant("GET")
                  .removeHeaders("FCREPO_IDENTIFIER")
                  .to("fcrepo:" + fcrepo_url + "?contentType=application/ld+json")
                  .to("mock:jsonld");

                from("direct:json2")
                  .setHeader("Exchange.HTTP_METHOD").constant("GET")
                  .removeHeaders("FCREPO_IDENTIFIER")
                  .setHeader("Exchange.CONTENT_TYPE").constant("application/ld+json")
                  .to("fcrepo:" + fcrepo_url)
                  .to("mock:jsonld");

                from("direct:json3")
                  .setHeader("Exchange.HTTP_METHOD").constant("GET")
                  .removeHeaders("FCREPO_IDENTIFIER")
                  .setHeader("Exchange.CONTENT_TYPE").constant("application/rdf+xml")
                  .to("fcrepo:" + fcrepo_url + "?contentType=application/ld+json")
                  .to("mock:jsonld");
            }
        };
    }
}
