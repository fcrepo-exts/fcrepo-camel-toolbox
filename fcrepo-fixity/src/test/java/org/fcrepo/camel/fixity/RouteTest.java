/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel.fixity;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.spring.javaconfig.CamelConfiguration;
import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import static org.apache.camel.component.mock.MockEndpoint.assertIsSatisfied;
import static org.apache.camel.util.ObjectHelper.loadResourceAsStream;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;

/**
 * Test the route workflow.
 *
 * @author Aaron Coburn
 * @since 2015-06-18
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {RouteTest.ContextConfig.class}, loader = AnnotationConfigContextLoader.class)
public class RouteTest {

    private static final long ASSERT_PERIOD_MS = 5000;
    @EndpointInject("mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce("direct:start")
    protected ProducerTemplate template;

    private static final String baseURL = "http://localhost/rest";
    private static final String identifier = "/file1";

    @Autowired
    private CamelContext camelContext;

    @Autowired
    private FcrepoFixityConfig config;

    @BeforeClass
    public static void beforeClass() {
        System.setProperty("fixity.failure", "mock:failure");
        System.setProperty("fixity.success", "mock:success");
        System.setProperty("fixity.input.stream", "seda:foo");
        System.setProperty("fixity.enabled", "true");

    }

    @Test
    public void testBinaryFixitySuccess() throws Exception {

        final var context = camelContext.adapt(ModelCamelContext.class);
        AdviceWith.adviceWith(context, "FcrepoFixity", a -> {
            a.replaceFromWith("direct:start");
            a.mockEndpointsAndSkip("fcrepo:*");
        });

        final var failureEndpoint = MockEndpoint.resolve(camelContext, config.getFixityFailure());
        failureEndpoint.expectedMessageCount(0);
        failureEndpoint.setAssertPeriod(ASSERT_PERIOD_MS);
        final var successEndpoint = MockEndpoint.resolve(camelContext, config.getFixitySuccess());
        successEndpoint.expectedMessageCount(1);

        final String body = IOUtils.toString(loadResourceAsStream("fixity.rdf"), "UTF-8");
        template.sendBodyAndHeader(body, FCREPO_URI, baseURL + identifier);

        assertIsSatisfied(failureEndpoint, successEndpoint);
    }

    @Test
    public void testBinaryFixityFailure() throws Exception {
        final var context = camelContext.adapt(ModelCamelContext.class);
        AdviceWith.adviceWith(context, "FcrepoFixity", a -> {
            a.replaceFromWith("direct:start");
            a.mockEndpointsAndSkip("fcrepo:*");
        });

        final var failureEndpoint = MockEndpoint.resolve(camelContext, "mock:failure");
        failureEndpoint.expectedMessageCount(1);
        final var successEndpoint = MockEndpoint.resolve(camelContext, "mock:success");
        successEndpoint.expectedMessageCount(0);
        successEndpoint.setAssertPeriod(ASSERT_PERIOD_MS);

        final String body = IOUtils.toString(loadResourceAsStream("fixityFailure.rdf"), "UTF-8");
        template.sendBodyAndHeader(body, FCREPO_URI, baseURL + identifier);

        assertIsSatisfied(failureEndpoint, successEndpoint);
    }

    @Test
    public void testNonBinary() throws Exception {
        final var context = camelContext.adapt(ModelCamelContext.class);
        AdviceWith.adviceWith(context, "FcrepoFixity", a -> {
            a.replaceFromWith("direct:start");
            a.mockEndpointsAndSkip("fcrepo:*");
        });

        final var failureEndpoint = MockEndpoint.resolve(camelContext, "mock:failure");
        failureEndpoint.expectedMessageCount(0);
        failureEndpoint.setAssertPeriod(ASSERT_PERIOD_MS);
        final var successEndpoint = MockEndpoint.resolve(camelContext, "mock:success");
        successEndpoint.expectedMessageCount(0);
        successEndpoint.setAssertPeriod(ASSERT_PERIOD_MS);

        final String body = IOUtils.toString(loadResourceAsStream("container.rdf"), "UTF-8");
        template.sendBodyAndHeader(body, FCREPO_URI, baseURL + identifier);
        assertIsSatisfied(failureEndpoint, successEndpoint);
    }

    @Configuration
    @ComponentScan(resourcePattern = "**/Fcrepo*.class")
    static class ContextConfig extends CamelConfiguration {
    }
}
