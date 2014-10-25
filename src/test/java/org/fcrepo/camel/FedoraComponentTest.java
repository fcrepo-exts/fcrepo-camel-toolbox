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
                from("timer://foo?repeatCount=1")
                  .setHeader("org.fcrepo.jms.baseURL").constant("http://localhost:8080/fcrepo4/rest")
                  .setHeader("org.fcrepo.jms.identifier").constant("/97/17/23/fe/971723fe-8ef4-43dc-8312-992be789f28d/ds4")
                  .to("fcrepo:bar?type=application/rdf+xml")
                  .to("mock:result")
                  .log("${body}");
            }
        };
    }
}
