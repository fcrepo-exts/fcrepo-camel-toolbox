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
package org.fcrepo.camel.integration;

import static org.apache.camel.Exchange.HTTP_METHOD;
import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.fcrepo.jms.headers.DefaultMessageFactory.IDENTIFIER_HEADER_NAME;
import static org.fcrepo.jms.headers.DefaultMessageFactory.BASE_URL_HEADER_NAME;
import static org.fcrepo.camel.FedoraEndpoint.FCREPO_IDENTIFIER;
import static org.fcrepo.camel.FedoraEndpoint.FCREPO_BASE_URL;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.fcrepo.camel.processor.SparqlDeleteProcessor;
import org.junit.Test;

/**
 * Test adding a non-RDF resource
 * @author Aaron Coburn
 * @since November 7, 2014
 */
public class SparqlDeleteProcessorTest extends CamelTestSupport {

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    @Test
    public void testDelete() throws IOException, InterruptedException {
        final String base = "http://localhost/rest";
        final String path = "/path/book3";
        final String incomingDoc = "_:B7a2aea322baa0335ba565419d128bd9e <http://www.w3.org/2001/vcard-rdf/3.0#FN> \"J.K. Rowling\" .\n" +
            "_:B7a2aea322baa0335ba565419d128bd9e <http://www.w3.org/2001/vcard-rdf/3.0#N> _:B4b31ccfac030360126a91c43e8ca732b .\n" +
            "_:B4b31ccfac030360126a91c43e8ca732b <http://www.w3.org/2001/vcard-rdf/3.0#Family> \"Rowling\" .\n" +
            "_:B4b31ccfac030360126a91c43e8ca732b <http://www.w3.org/2001/vcard-rdf/3.0#Given> \"Joanna\" .\n" +
            "<" + base + path + "> <http://purl.org/dc/elements/1.1/title> \"Harry Potter and the Prisoner Of Azkaban\" .\n" +
            "<" + base + path + "> <http://purl.org/dc/elements/1.1/relation> <" + base + path + "/appendix> .\n" +
            "<" + base + path + "> <http://purl.org/dc/elements/1.1/creator> _:B7a2aea322baa0335ba565419d128bd9e .";
        
        // Assertions
        resultEndpoint.expectedBodiesReceived("DELETE WHERE { <" + base + path + "> ?p ?o };\nDELETE WHERE { <" + base + path + "/appendix> ?p ?o }");
        resultEndpoint.expectedHeaderReceived(CONTENT_TYPE, "application/sparql-update");
        resultEndpoint.expectedHeaderReceived(HTTP_METHOD, "POST");

        // Test
        final Map<String, Object> headers = new HashMap<>();
        headers.put(FCREPO_BASE_URL, base);
        headers.put(FCREPO_IDENTIFIER, path);
        template.sendBodyAndHeaders(incomingDoc, headers);

        headers.clear();
        headers.put(BASE_URL_HEADER_NAME, base);
        headers.put(IDENTIFIER_HEADER_NAME, path);
        template.sendBodyAndHeaders(incomingDoc, headers);

        headers.clear();
        headers.put(BASE_URL_HEADER_NAME, base);
        headers.put(FCREPO_IDENTIFIER, path);
        template.sendBodyAndHeaders(incomingDoc, headers);

        headers.clear();
        headers.put(FCREPO_BASE_URL, base);
        headers.put(IDENTIFIER_HEADER_NAME, path);
        template.sendBodyAndHeaders(incomingDoc, headers);

        // Confirm that assertions passed
        resultEndpoint.expectedMessageCount(4);
        resultEndpoint.assertIsSatisfied();
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() throws IOException {
                from("direct:start")
                    .process(new SparqlDeleteProcessor())
                    .to("mock:result");
            }
        };
    }
}
