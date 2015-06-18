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
package org.fcrepo.camel.fixity.integration;

import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.camel.util.ObjectHelper;
import org.apache.commons.codec.digest.DigestUtils;
import org.fcrepo.camel.FcrepoClient;
import org.fcrepo.camel.FcrepoHeaders;
import org.fcrepo.camel.FcrepoResponse;
import org.fcrepo.camel.RdfNamespaces;
import org.junit.Test;

/**
 * Test the route workflow.
 *
 * @author Aaron Coburn
 * @since 2015-06-18
 */
public class RouteIT extends CamelBlueprintTestSupport {

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    private String fullPath = "";
    private String digest = "";
    private final String binary = "binary.txt";

    @Override
    protected void doPreSetup() throws Exception {
        final String webPort = System.getProperty("fcrepo.dynamic.test.port", "8080");
        final FcrepoClient client = new FcrepoClient(null, null, null, true);
        final FcrepoResponse res = client.post(
                URI.create("http://localhost:" + webPort + "/fcrepo/rest"),
                ObjectHelper.loadResourceAsStream(binary), "text/plain");
        fullPath = res.getLocation().toString();

        digest = DigestUtils.sha1Hex(ObjectHelper.loadResourceAsStream(binary));
    }

    @Override
    public boolean isUseAdviceWith() {
        return true;
    }

    @Override
    public boolean isUseRouteBuilder() {
        return false;
    }

    @Override
    protected String getBlueprintDescriptor() {
        return "/OSGI-INF/blueprint/blueprint.xml";
    }

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        final String webPort = System.getProperty("fcrepo.dynamic.test.port", "8080");

        final Properties props = new Properties();
        props.put("fcrepo.baseUrl", "localhost:" + webPort + "/fcrepo/rest");
        props.put("fixity.stream", "direct:start");
        props.put("fixity.failure", "mock:failure");
        props.put("fixity.success", "mock:success");
        return props;
    }

    @Test
    public void testFixityOnBinary() throws Exception {
        final String webPort = System.getProperty("fcrepo.dynamic.test.port", "8080");
        final String jmsPort = System.getProperty("fcrepo.dynamic.jms.port", "61616");
        final String path = fullPath.replaceFirst("http://localhost:[0-9]+/fcrepo/rest", "");
        final String fcrepoEndpoint = "mock:fcrepo:localhost:" + webPort + "/fcrepo/rest";
        final Namespaces ns = new Namespaces("rdf", RdfNamespaces.RDF);
        ns.add("fedora", RdfNamespaces.REPOSITORY);
        ns.add("premis", RdfNamespaces.PREMIS);

        context.getRouteDefinition("FcrepoFixity").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                mockEndpoints("*");
            }
        });
        context.start();

        final Map<String, Object> headers = new HashMap<>();
        headers.put(FcrepoHeaders.FCREPO_IDENTIFIER, path);
        headers.put(FcrepoHeaders.FCREPO_BASE_URL, "http://localhost:" + webPort + "/fcrepo/rest");

        getMockEndpoint(fcrepoEndpoint).expectedMessageCount(2);
        getMockEndpoint("mock:success").expectedMessageCount(1);

        template.sendBodyAndHeaders("direct:start", "", headers);

        assertMockEndpointsSatisfied();

        final String body = getMockEndpoint("mock:success").assertExchangeReceived(0).getIn().getBody(String.class);
        assertTrue(body.contains(
                "<premis:hasSize rdf:datatype=\"http://www.w3.org/2001/XMLSchema#long\">74</premis:hasSize>"));
        assertTrue(body.contains(
                "<premis:hasMessageDigest rdf:resource=\"urn:sha1:" + digest + "\"/>"));
    }
}
