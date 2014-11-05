/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/license/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software     
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

public class FedoraContentTypeEndpointTest extends CamelTestSupport {

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    @Test
    public void testContentTypeTurtle() throws Exception {
        resultEndpoint.expectedHeaderReceived("Content-Type", "text/turtle");
        resultEndpoint.expectedMessageCount(1);

        template.sendBody(null);

        resultEndpoint.assertIsSatisfied();
    }

    @Test
    public void testContentTypeN3() throws Exception {
        resultEndpoint.expectedHeaderReceived("Content-Type", "text/turtle");
        resultEndpoint.expectedMessageCount(2);

        template.sendBodyAndHeader(null, "Accept", "application/n-triples");
        template.sendBodyAndHeader(null, Exchange.ACCEPT_CONTENT_TYPE, "application/n-triples");

        resultEndpoint.assertIsSatisfied();
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() throws IOException {
                final String fcrepo_uri = FedoraTestUtils.getFcrepoEndpointUri();

                from("direct:start")
                    .to(fcrepo_uri + "?accept=text/turtle")
                    .to("mock:result");
            }
        };
    }
}


