/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel.fixity.integration;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.spring.javaconfig.CamelConfiguration;
import org.apache.camel.support.builder.Namespaces;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.jena.vocabulary.RDF;
import org.fcrepo.camel.fixity.FcrepoFixityConfig;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoResponse;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.net.URI;

import static junit.framework.TestCase.assertTrue;
import static org.apache.camel.component.mock.MockEndpoint.assertIsSatisfied;
import static org.apache.camel.util.ObjectHelper.loadResourceAsStream;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.fcrepo.client.FcrepoClient.client;

/**
 * Test the route workflow.
 *
 * @author Aaron Coburn
 * @since 2015-06-18
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {RouteIT.ContextConfig.class}, loader = AnnotationConfigContextLoader.class)
public class RouteIT {
    private static Logger LOGGER = LoggerFactory.getLogger(RouteIT.class);
    private static final String REPOSITORY = "http://fedora.info/definitions/v4/repository#";

    private static String FEDORA_USERNAME = "fedoraAdmin";
    private static String FEDORA_PASSWORD = "fedoraAdmin";

    private static final String binary = "binary.txt";
    private static String fullPath = "";
    @EndpointInject("mock:result")
    protected MockEndpoint resultEndpoint;
    @Produce("direct:start")
    protected ProducerTemplate template;
    @Autowired
    private CamelContext camelContext;

    @Autowired
    private FcrepoFixityConfig config;

    @BeforeClass
    public static void beforeClass() throws Exception {
        final String webPort = System.getProperty("fcrepo.dynamic.test.port", "8080");
        final String jmsPort = System.getProperty("fcrepo.dynamic.jms.port", "61616");

        final FcrepoClient client = client().throwExceptionOnFailure()
                .credentials(FEDORA_USERNAME, FEDORA_PASSWORD).build();
        final FcrepoResponse res = client.post(URI.create("http://localhost:" + webPort + "/fcrepo/rest"))
                .body(loadResourceAsStream(binary), "text/plain").perform();
        fullPath = res.getLocation().toString();

        System.setProperty("fixity.failure", "mock:failure");
        System.setProperty("fixity.success", "mock:success");
        System.setProperty("fixity.input.stream", "direct:start");

        System.setProperty("fcrepo.baseUrl", "http://localhost:" + webPort + "/fcrepo/rest");
        System.setProperty("jms.brokerUrl", "tcp://localhost:" + jmsPort);
        System.setProperty("fixity.enabled", "true");

    }

    @Test
    public void testFixityOnBinary() throws Exception {
        final String webPort = System.getProperty("fcrepo.dynamic.test.port", "8080");
        final String path = fullPath.replaceFirst("http://localhost:[0-9]+/fcrepo/rest", "");
        final String fcrepoEndpoint = "mock:fcrepo:http://localhost:" + webPort + "/fcrepo/rest";
        final Namespaces ns = new Namespaces("rdf", RDF.uri);
        ns.add("fedora", REPOSITORY);
        ns.add("premis", "http://www.loc.gov/premis/rdf/v1#");

        final var digest = DigestUtils.sha512Hex(loadResourceAsStream(binary));

        LOGGER.info("digest={}", digest);
        final var context = camelContext.adapt(ModelCamelContext.class);
        AdviceWith.adviceWith(context, "FcrepoFixity", a -> {
            a.mockEndpoints("*");
        });

        final var fcrepoEndpointObj = MockEndpoint.resolve(camelContext, fcrepoEndpoint);
        fcrepoEndpointObj.expectedMessageCount(2);
        final var successEndpoint = MockEndpoint.resolve(camelContext, "mock:success");
        successEndpoint.expectedMessageCount(1);
        template.sendBodyAndHeader("direct:start", "", FCREPO_URI,
                config.getFcrepoBaseUrl() + path);

        assertIsSatisfied(fcrepoEndpointObj, successEndpoint);

        final String body = successEndpoint.assertExchangeReceived(0).getIn().getBody(String.class);

        LOGGER.info("body={}", body);
        assertTrue(body.contains(
                "<premis:hasSize rdf:datatype=\"http://www.w3.org/2001/XMLSchema#long\">74</premis:hasSize>"));
        assertTrue(body.contains(
                "<premis:hasMessageDigest rdf:resource=\"urn:sha-512:" + digest + "\"/>"));
    }

    @Configuration
    @ComponentScan("org.fcrepo.camel")
    static class ContextConfig extends CamelConfiguration {

    }
}
