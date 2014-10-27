package org.fcrepo.camel;

import org.apache.camel.Processor;
import org.apache.camel.Message;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

public class FedoraComponentTest extends CamelTestSupport {

    @Test
    public void testFedora() throws Exception {
        MockEndpoint mockJsonLd = getMockEndpoint("mock:jsonld");
        mockJsonLd.expectedMessageCount(3);
        mockJsonLd.expectedHeaderReceived("Content-Type", "application/ld+json");

        MockEndpoint mockXml = getMockEndpoint("mock:xml");
        mockXml.expectedMessageCount(1);
        mockXml.expectedHeaderReceived("Content-Type", "application/rdf+xml");
        
        MockEndpoint mockDatastream = getMockEndpoint("mock:datastream");
        mockDatastream.expectedHeaderReceived("Content-Type", "application/rdf+xml");
        mockDatastream.expectedMessageCount(1);

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
            public void configure() {

                Namespaces ns = new Namespaces("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
                
                from("timer://foo?repeatCount=1")
                  .multicast().to("direct:json", "direct:xml", "direct:post", "direct:put")
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
                    .to("fcrepo:localhost:8080/fcrepo4/rest/")
                    .to("mock:post")
                    .setHeader("Exchange.HTTP_METHOD").constant("DELETE")
                    .setHeader("FCREPO_IDENTIFIER").simple("${body.replaceAll(\"http://localhost:8080/fcrepo4/rest\", \"\")}")
                    .to("fcrepo:localhost:8080/fcrepo4/rest")
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
                    .to("fcrepo:localhost:8080/fcrepo4/rest")
                    .to("mock:put")
                    .setHeader("Exchange.HTTP_METHOD").constant("PATCH")
                    .process(new Processor() {
                        public void process(Exchange exchange) {
                            Message in = exchange.getIn();
                            in.setBody("PREFIX dc: <http://purl.org/dc/elements/1.1/> \n\nINSERT { <> dc:title \"some-resource-title\" . } \nWHERE { }");
                        }})
                    .to("fcrepo:localhost:8080/fcrepo4/rest")
                    .to("mock:patch")
                    .setHeader("FCREPO_IDENTIFIER").simple("/testing/object3")
                    .setHeader("Exchange.HTTP_METHOD").constant("DELETE")
                    .to("fcrepo:localhost:8080/fcrepo4/rest")
                    .setHeader("FCREPO_IDENTIFIER").simple("/testing/object3/fcr:tombstone")
                    .setHeader("Exchange.HTTP_METHOD").constant("DELETE")
                    .to("fcrepo:localhost:8080/fcrepo4/rest")
                    .to("mock:delete");

                from("direct:json")
                    .multicast().to("direct:json1", "direct:json2", "direct:json3");

                from("direct:xml")
                    .multicast().to("direct:xml1", "direct:xml2", "direct:xml3");

                from("direct:xml1")
                  .to("fcrepo:localhost:8080/fcrepo4/rest")
                  .to("mock:xml");
                    
                from("direct:xml2")
                  .setHeader("org.fcrepo.jms.identifier").constant("/97/17/23/fe/971723fe-8ef4-43dc-8312-992be789f28d/ds4")
                  .to("fcrepo:localhost:8080/fcrepo4/rest")
                  .filter().xpath("/rdf:RDF/rdf:Description/rdf:type[@rdf:resource='http://fedora.info/definitions/v4/rest-api#resource']", ns)
                  .to("mock:resource");
                
                from("direct:xml3")
                  .setHeader("org.fcrepo.jms.identifier").constant("/97/17/23/fe/971723fe-8ef4-43dc-8312-992be789f28d/ds4")
                  .to("fcrepo:localhost:8080/fcrepo4/rest")
                  .filter().xpath("/rdf:RDF/rdf:Description/rdf:type[@rdf:resource='http://fedora.info/definitions/v4/rest-api#datastream']", ns)
                  .to("mock:datastream");


                from("direct:json1")
                  .setHeader("org.fcrepo.jms.identifier").constant("/97/17/23/fe/971723fe-8ef4-43dc-8312-992be789f28d/ds4")
                  .to("fcrepo:localhost:8080/fcrepo4/rest?type=application/ld+json")
                  .to("mock:jsonld");

                from("direct:json2")
                  .setHeader("Exchange.CONTENT_TYPE").constant("application/ld+json")
                  .to("fcrepo:localhost:8080/fcrepo4/rest")
                  .to("mock:jsonld");

                from("direct:json3")
                  .setHeader("Exchange.CONTENT_TYPE").constant("application/rdf+xml")
                  .to("fcrepo:localhost:8080/fcrepo4/rest?type=application/ld+json")
                  .to("mock:jsonld");
            }
        };
    }
}
