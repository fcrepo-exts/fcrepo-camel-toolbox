/**
 * Copyright 2014 DuraSpace, Inc.
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
package org.fcrepo.camel;

import static org.apache.camel.Exchange.ACCEPT_CONTENT_TYPE;
import static org.fcrepo.camel.FedoraEndpoint.DEFAULT_CONTENT_TYPE;
import static org.fcrepo.camel.FedoraTestUtils.getFcrepoEndpointUri;

import java.io.IOException;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

public class FedoraContentTypeHeaderTest extends CamelTestSupport {

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    @Test
    public void testContentTypeJson() throws InterruptedException {
        resultEndpoint.expectedHeaderReceived("Content-Type", "application/ld+json");
        resultEndpoint.expectedMessageCount(2);

        template.sendBodyAndHeader(null, "Accept", "application/ld+json");
        template.sendBodyAndHeader(null, ACCEPT_CONTENT_TYPE, "application/ld+json");

        resultEndpoint.assertIsSatisfied();
    }

    @Test
    public void testContentTypeRdfXml() throws InterruptedException {
        resultEndpoint.expectedHeaderReceived("Content-Type", "application/rdf+xml");
        resultEndpoint.expectedMessageCount(2);

        template.sendBodyAndHeader(null, "Accept", "application/rdf+xml");
        template.sendBodyAndHeader(null, ACCEPT_CONTENT_TYPE, "application/rdf+xml");

        resultEndpoint.assertIsSatisfied();
    }

    @Test
    public void testContentTypeNTriples() throws InterruptedException  {
        resultEndpoint.expectedHeaderReceived("Content-Type", "application/n-triples");
        resultEndpoint.expectedMessageCount(2);

        template.sendBodyAndHeader(null, "Accept", "application/n-triples");
        template.sendBodyAndHeader(null, ACCEPT_CONTENT_TYPE, "application/n-triples");

        resultEndpoint.assertIsSatisfied();
    }

    @Test
    public void testContentTypeTurtle() throws InterruptedException {
        resultEndpoint.expectedHeaderReceived("Content-Type", "text/turtle");
        resultEndpoint.expectedMessageCount(2);

        template.sendBodyAndHeader(null, "Accept", "text/turtle");
        template.sendBodyAndHeader(null, ACCEPT_CONTENT_TYPE, "text/turtle");

        resultEndpoint.assertIsSatisfied();
    }

    @Test
    public void testContentTypeN3() throws InterruptedException {
        resultEndpoint.expectedHeaderReceived("Content-Type", "text/rdf+n3");
        resultEndpoint.expectedMessageCount(2);

        template.sendBodyAndHeader(null, "Accept", "text/rdf+n3");
        template.sendBodyAndHeader(null, ACCEPT_CONTENT_TYPE, "text/rdf+n3");

        resultEndpoint.assertIsSatisfied();
    }


    @Test
    public void testContentTypeDefault() throws InterruptedException {
        resultEndpoint.expectedHeaderReceived("Content-Type", DEFAULT_CONTENT_TYPE);
        resultEndpoint.expectedMessageCount(1);

        template.sendBody(null);

        resultEndpoint.assertIsSatisfied();
    }


    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() throws IOException {
                final String fcrepo_uri = getFcrepoEndpointUri();

                from("direct:start")
                    .to(fcrepo_uri)
                    .to("mock:result");
            }
        };
    }
}
