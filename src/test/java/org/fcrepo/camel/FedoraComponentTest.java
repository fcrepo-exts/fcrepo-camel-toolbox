package org.fcrepo.camel;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

public class FedoraComponentTest extends CamelTestSupport {

    @Test
    public void testFedora() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMinimumMessageCount(1);       
        
        assertMockEndpointsSatisfied();
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() {
                
                //from("timer://foo?repeatCount=1")
                from("fcrepo://localhost:8080/fcrepo4/rest/?type=text/turtle")
                  .setHeader("org.fcrepo.jms.baseURL").constant("http://localhost:8080/fcrepo4/rest")
                  .setHeader("org.fcrepo.jms.identifier").constant("/")
                  .to("fcrepo:bar?type=text/turtle")
                  .to("mock:result")
                  .log("${body}");
            }
        };
    }
}
