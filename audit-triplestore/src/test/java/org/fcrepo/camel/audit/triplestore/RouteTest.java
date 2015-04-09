/**
 * Copyright 2015 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.camel.audit.triplestore;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.fcrepo.camel.JmsHeaders;
import org.fcrepo.camel.RdfNamespaces;

import org.junit.Test;

/**
 * Test the route workflow.
 *
 * @author Aaron Coburn
 */
public class RouteTest extends CamelBlueprintTestSupport {

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    @Override
    protected String getBlueprintDescriptor() {
        return "/OSGI-INF/blueprint/blueprint.xml";
    }

    @Test
    public void testEventProcessor() throws Exception {

        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.expectedHeaderReceived(Exchange.CONTENT_TYPE, "application/x-www-form-urlencoded");
        resultEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");

        final Map<String, Object> headers = new HashMap<>();
        headers.put(JmsHeaders.BASE_URL, "http://localhost/rest");
        headers.put(JmsHeaders.IDENTIFIER, "/foo");
        headers.put(JmsHeaders.TIMESTAMP, "1428360320168");
        headers.put(JmsHeaders.EVENT_TYPE, RdfNamespaces.REPOSITORY + "NODE_ADDED");
        headers.put(JmsHeaders.USER, "bypassAdmin");
        headers.put(JmsHeaders.USER_AGENT, "curl/7.37.1");
        template.sendBodyAndHeaders(null, headers);

        // assert expectations
        assertMockEndpointsSatisfied();
    }


    @Test
    public void testFixityProcessor() throws Exception {

        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.expectedHeaderReceived(Exchange.CONTENT_TYPE, "application/x-www-form-urlencoded");
        resultEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");

        final Map<String, Object> headers = new HashMap<>();
        headers.put(JmsHeaders.BASE_URL, "http://localhost/rest");
        headers.put(JmsHeaders.IDENTIFIER, "/foo");
        headers.put(JmsHeaders.TIMESTAMP, "1428360320168");
        headers.put(JmsHeaders.EVENT_TYPE, RdfNamespaces.REPOSITORY + "FIXITY");
        headers.put("org.fcrepo.jms.fixity", "foo");
        headers.put("org.fcrepo.jms.contentDigest", "blahblahblah");
        headers.put("org.fcrepo.jms.contentSize", "100");
        headers.put("org.fcrepo.jms.user", "bypassAdmin");
        headers.put("org.fcrepo.jms.userAgent", "curl/7.37.1");
        template.sendBodyAndHeaders(null, headers);

        // assert expectations
        assertMockEndpointsSatisfied();
    }


    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() throws IOException {
                from("direct:start")
                    .process(new AuditSparqlProcessor())
                    .to("mock:result");
            }
        };
    }
}
