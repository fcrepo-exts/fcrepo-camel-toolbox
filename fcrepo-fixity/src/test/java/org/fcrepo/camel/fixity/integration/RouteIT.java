/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
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

import static org.apache.camel.util.ObjectHelper.loadResourceAsStream;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.fcrepo.client.FcrepoClient.client;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.Dictionary;
import java.util.Map;
import java.util.Properties;

import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.camel.util.KeyValueHolder;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.jena.vocabulary.RDF;
import org.fcrepo.camel.FcrepoComponent;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoResponse;
import org.junit.Test;

/**
 * Test the route workflow.
 *
 * @author Aaron Coburn
 * @since 2015-06-18
 */
public class RouteIT extends CamelBlueprintTestSupport {

    private static final String REPOSITORY = "http://fedora.info/definitions/v4/repository#";

    private static String FEDORA_USERNAME = "fedoraAdmin";
    private static String FEDORA_PASSWORD = "fedoraAdmin";

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
        final FcrepoClient client = client().throwExceptionOnFailure()
                                            .credentials(FEDORA_USERNAME, FEDORA_PASSWORD).build();
        final FcrepoResponse res = client.post(URI.create("http://localhost:" + webPort + "/fcrepo/rest"))
                                .body(loadResourceAsStream(binary), "text/plain").perform();
        fullPath = res.getLocation().toString();

        digest = DigestUtils.sha1Hex(loadResourceAsStream(binary));
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
        return "/OSGI-INF/blueprint/blueprint-test.xml";
    }

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        final Properties props = new Properties();
        props.put("fixity.stream", "direct:start");
        props.put("fixity.failure", "mock:failure");
        props.put("fixity.success", "mock:success");
        return props;
    }

    @Override
    protected void addServicesOnStartup(final Map<String, KeyValueHolder<Object, Dictionary>> services) {
        final String jmsPort = System.getProperty("fcrepo.dynamic.jms.port", "61616");
        final String webPort = System.getProperty("fcrepo.dynamic.test.port", "8080");
        final ActiveMQComponent component = new ActiveMQComponent();

        component.setBrokerURL("tcp://localhost:" + jmsPort);
        component.setExposeAllQueues(true);

        final FcrepoComponent fcrepo = new FcrepoComponent();
        fcrepo.setBaseUrl("http://localhost:" + webPort + "/fcrepo/rest");
        fcrepo.setAuthUsername(FEDORA_USERNAME);
        fcrepo.setAuthPassword(FEDORA_PASSWORD);
        services.put("broker", asService(component, "osgi.jndi.service.name", "fcrepo/Broker"));
        services.put("fcrepo", asService(fcrepo, "osgi.jndi.service.name", "fcrepo/Camel"));
    }

    @Test
    public void testFixityOnBinary() throws Exception {
        final String webPort = System.getProperty("fcrepo.dynamic.test.port", "8080");
        final String jmsPort = System.getProperty("fcrepo.dynamic.jms.port", "61616");
        final String path = fullPath.replaceFirst("http://localhost:[0-9]+/fcrepo/rest", "");
        final String fcrepoEndpoint = "mock:fcrepo:http://localhost:8080/fcrepo/rest";
        final Namespaces ns = new Namespaces("rdf", RDF.uri);
        ns.add("fedora", REPOSITORY);
        ns.add("premis", "http://www.loc.gov/premis/rdf/v1#");

        context.getRouteDefinition("FcrepoFixity").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                mockEndpoints("*");
            }
        });
        context.start();

        getMockEndpoint(fcrepoEndpoint).expectedMessageCount(2);
        getMockEndpoint("mock:success").expectedMessageCount(1);
        template.sendBodyAndHeader("direct:start", "", FCREPO_URI,
                "http://localhost:" + webPort + "/fcrepo/rest" + path);

        assertMockEndpointsSatisfied();

        final String body = getMockEndpoint("mock:success").assertExchangeReceived(0).getIn().getBody(String.class);
        assertTrue(body.contains(
                "<premis:hasSize rdf:datatype=\"http://www.w3.org/2001/XMLSchema#long\">74</premis:hasSize>"));
        assertTrue(body.contains(
                "<premis:hasMessageDigest rdf:resource=\"urn:sha1:" + digest + "\"/>"));
    }
}
