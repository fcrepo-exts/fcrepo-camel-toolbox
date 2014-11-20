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

import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.Exchange.HTTP_METHOD;
import static org.fcrepo.camel.FedoraEndpoint.FCREPO_IDENTIFIER;
import static org.fcrepo.camel.integration.FedoraTestUtils.getFcrepoBaseUrl;
import static org.fcrepo.camel.integration.FedoraTestUtils.getFcrepoEndpointUri;
import static org.fcrepo.camel.integration.FedoraTestUtils.getTurtleDocument;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.builder.xml.XPathBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Test adding a new resource with POST
 * @author Aaron Coburn
 * @since November 7, 2014
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/spring-test/test-container.xml"})
public class FedoraAuthTest extends CamelTestSupport {

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    @Test
    public void testAuth() throws InterruptedException {
        // Assertions
        resultEndpoint.expectedBodiesReceived("some title");

        // Setup
        final Map<String, Object> headers = new HashMap<>();
        headers.put(HTTP_METHOD, "POST");
        headers.put(CONTENT_TYPE, "text/turtle");

        final String fullPath = template.requestBodyAndHeaders(
                "direct:setup", getTurtleDocument(), headers, String.class);

        final String identifier = fullPath.replaceAll(getFcrepoBaseUrl(), "");

        // Test
        template.sendBodyAndHeader("direct:auth1", null, FCREPO_IDENTIFIER, identifier);
        template.sendBodyAndHeader("direct:auth2", null, FCREPO_IDENTIFIER, identifier);
        template.sendBodyAndHeader("direct:auth3", null, FCREPO_IDENTIFIER, identifier);
        template.sendBodyAndHeader("direct:auth4", null, FCREPO_IDENTIFIER, identifier);
        template.sendBodyAndHeader("direct:auth5", null, FCREPO_IDENTIFIER, identifier);
        template.sendBodyAndHeader("direct:auth6", null, FCREPO_IDENTIFIER, identifier);
        template.sendBodyAndHeader("direct:auth7", null, FCREPO_IDENTIFIER, identifier);
        template.sendBodyAndHeader("direct:auth8", null, FCREPO_IDENTIFIER, identifier);

        // Teardown
        final Map<String, Object> teardownHeaders = new HashMap<>();
        teardownHeaders.put(HTTP_METHOD, "DELETE");
        teardownHeaders.put(FCREPO_IDENTIFIER, identifier);
        template.sendBodyAndHeaders("direct:teardown", null, teardownHeaders);

        // Confirm that the assertions passed
        resultEndpoint.expectedMessageCount(8);
        resultEndpoint.assertIsSatisfied();
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                final String fcrepo_uri = getFcrepoEndpointUri();

                final Namespaces ns = new Namespaces("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");

                final XPathBuilder titleXpath = new XPathBuilder("/rdf:RDF/rdf:Description/dc:title/text()");
                titleXpath.namespaces(ns);
                titleXpath.namespace("dc", "http://purl.org/dc/elements/1.1/");

                from("direct:setup")
                    .to(fcrepo_uri);

                from("direct:auth1")
                    .to(fcrepo_uri + "?authUsername=foo")
                    .filter().xpath(
                        "/rdf:RDF/rdf:Description/rdf:type" +
                        "[@rdf:resource='http://fedora.info/definitions/v4/repository#Resource']", ns)
                    .split(titleXpath)
                    .to("mock:result");

                from("direct:auth2")
                    .to(fcrepo_uri + "?authPassword=foo")
                    .filter().xpath(
                        "/rdf:RDF/rdf:Description/rdf:type" +
                        "[@rdf:resource='http://fedora.info/definitions/v4/repository#Resource']", ns)
                    .split(titleXpath)
                    .to("mock:result");

                from("direct:auth3")
                    .to(fcrepo_uri + "?authPassword=foo&authUsername=")
                    .filter().xpath(
                        "/rdf:RDF/rdf:Description/rdf:type" +
                        "[@rdf:resource='http://fedora.info/definitions/v4/repository#Resource']", ns)
                    .split(titleXpath)
                    .to("mock:result");

                from("direct:auth4")
                    .to(fcrepo_uri + "?authPassword=&authUsername=")
                    .filter().xpath(
                        "/rdf:RDF/rdf:Description/rdf:type" +
                        "[@rdf:resource='http://fedora.info/definitions/v4/repository#Resource']", ns)
                    .split(titleXpath)
                    .to("mock:result");

                from("direct:auth5")
                    .to(fcrepo_uri + "?authPassword=")
                    .filter().xpath(
                        "/rdf:RDF/rdf:Description/rdf:type" +
                        "[@rdf:resource='http://fedora.info/definitions/v4/repository#Resource']", ns)
                    .split(titleXpath)
                    .to("mock:result");

                from("direct:auth6")
                    .to(fcrepo_uri + "?authUsername=")
                    .filter().xpath(
                        "/rdf:RDF/rdf:Description/rdf:type" +
                        "[@rdf:resource='http://fedora.info/definitions/v4/repository#Resource']", ns)
                    .split(titleXpath)
                    .to("mock:result");

                from("direct:auth7")
                    .to(fcrepo_uri + "?authUsername=foo&authPassword=bar&authHost=localhost")
                    .filter().xpath(
                        "/rdf:RDF/rdf:Description/rdf:type" +
                        "[@rdf:resource='http://fedora.info/definitions/v4/repository#Resource']", ns)
                    .split(titleXpath)
                    .to("mock:result");

                from("direct:auth8")
                    .to(fcrepo_uri + "?authUsername=foo&authPassword=bar")
                    .filter().xpath(
                        "/rdf:RDF/rdf:Description/rdf:type" +
                        "[@rdf:resource='http://fedora.info/definitions/v4/repository#Resource']", ns)
                    .split(titleXpath)
                    .to("mock:result");



                from("direct:teardown")
                    .to(fcrepo_uri)
                    .to(fcrepo_uri + "?tombstone=true");
            }
        };
    }
}
