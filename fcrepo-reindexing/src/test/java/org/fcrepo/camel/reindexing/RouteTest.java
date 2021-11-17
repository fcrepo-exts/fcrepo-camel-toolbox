/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel.reindexing;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.TransformDefinition;
import org.apache.camel.spring.javaconfig.CamelConfiguration;
import org.apache.camel.util.ObjectHelper;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.fcrepo.camel.reindexing.ReindexingHeaders.REINDEXING_RECIPIENTS;

/**
 * Test the route workflow.
 *
 * @author acoburn
 * @since 2015-05-22
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {RouteTest.ContextConfig.class}, loader = AnnotationConfigContextLoader.class)
public class RouteTest {

    private static final String restPrefix = "/reindexing";
    private static final String reindexingStream = "broker:queue:foo";
    private static final String baseUrl = "http://localhost/rest";
    private long ASSERT_PERIOD_MS = 5000;

    @EndpointInject("mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce("direct:start")
    protected ProducerTemplate template;

    @Autowired
    private CamelContext camelContext;

    @BeforeClass
    public static void beforeClass() {
        final String restPort = System.getProperty("fcrepo.dynamic.reindexing.port");
        if (!isBlank(restPort)) {
            System.setProperty("reindexing.stream", reindexingStream);
            System.setProperty("reindexing.rest.port", restPort);
            System.setProperty("fcrepo.baseUrl", baseUrl);

        }
        System.setProperty("reindexing.rest.prefix", restPrefix);
    }

    @DirtiesContext
    @Test
    public void testUsageRoute() throws Exception {

        final String restPort = System.getProperty("fcrepo.dynamic.reindexing.port", "9080");

        final var context = camelContext.adapt(ModelCamelContext.class);

        AdviceWith.adviceWith(context, "FcrepoReindexingTraverse", a -> {
            a.replaceFromWith("direct:traverse");
            a.mockEndpointsAndSkip("broker:*");
            a.mockEndpointsAndSkip("fcrepo:*");
        });

        AdviceWith.adviceWith(context, "FcrepoReindexingUsage", a -> {
            a.weaveAddLast().to("mock:result");
        });

        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.message(0).body().contains("Fedora Reindexing Service");
        resultEndpoint.message(0).body().contains(
                InetAddress.getLocalHost().getHostName() + ":" + restPort + restPrefix);

        template.sendBody("direct:usage", null);

        MockEndpoint.assertIsSatisfied(resultEndpoint);
    }

    @DirtiesContext
    @Test
    public void testReindexNoEndpointsRoute() throws Exception {
        final String url = "http://localhost:8080/fcrepo/rest/foo";

        final var context = camelContext.adapt(ModelCamelContext.class);

        AdviceWith.adviceWith(context, "FcrepoReindexingReindex", a -> {
            a.mockEndpointsAndSkip(reindexingStream + "?disableTimeToLive=true");
            a.weaveByType(TransformDefinition.class).after().to("mock:result");
        });

        AdviceWith.adviceWith(context, "FcrepoReindexingTraverse", a -> {
            a.replaceFromWith("direct:traverse");
            a.mockEndpointsAndSkip("broker:*");
            a.mockEndpointsAndSkip("fcrepo:*");
        });

        final var reindexingEndpoint = MockEndpoint.resolve(camelContext, "mock:" + reindexingStream);
        reindexingEndpoint.expectedMessageCount(0);
        reindexingEndpoint.setAssertPeriod(ASSERT_PERIOD_MS);
        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.expectedHeaderReceived(FCREPO_URI, url);
        resultEndpoint.expectedBodiesReceived("No endpoints configured for indexing");

        final Map<String, Object> headers = new HashMap<>();
        headers.put(FCREPO_URI, url);
        headers.put(REINDEXING_RECIPIENTS, "");

        template.sendBodyAndHeaders("direct:reindex", null, headers);

        MockEndpoint.assertIsSatisfied(resultEndpoint, reindexingEndpoint);
    }

    @DirtiesContext
    @Test
    public void testReindexWithEndpointsRoute() throws Exception {
        final String url = "http://localhost:8080/fcrepo/rest/foo";

        final var context = camelContext.adapt(ModelCamelContext.class);

        AdviceWith.adviceWith(context, "FcrepoReindexingReindex", a -> {
            a.mockEndpointsAndSkip(reindexingStream + "?disableTimeToLive=true");
            a.weaveByType(TransformDefinition.class).after().to("mock:result");
        });

        AdviceWith.adviceWith(context, "FcrepoReindexingTraverse", a -> {
            a.replaceFromWith("direct:traverse");
            a.mockEndpointsAndSkip("broker:*");
            a.mockEndpointsAndSkip("fcrepo:*");
        });

        final var reindexingEndpoint = MockEndpoint.resolve(camelContext, "mock:" + reindexingStream);
        reindexingEndpoint.expectedMessageCount(1);
        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.expectedHeaderReceived(FCREPO_URI, url);
        resultEndpoint.expectedBodiesReceived("Indexing started at " + url);

        final Map<String, Object> headers = new HashMap<>();
        headers.put(FCREPO_URI, url);
        headers.put(REINDEXING_RECIPIENTS, "mock:endpoint");

        template.sendBodyAndHeaders("direct:reindex", null, headers);

        MockEndpoint.assertIsSatisfied(resultEndpoint, reindexingEndpoint);
    }

    @DirtiesContext
    @Test
    public void testTraversal() throws Exception {

        final String baseUrl = "http://localhost:8080/fcrepo4/rest";

        final var recipientsEndpoint = MockEndpoint.resolve(camelContext, "mock:direct:recipients");
        final var reindexingEndpoint = MockEndpoint.resolve(camelContext, "mock:" + reindexingStream);

        recipientsEndpoint.expectedMessageCount(1);
        reindexingEndpoint.expectedMessageCount(7);
        reindexingEndpoint.expectedHeaderValuesReceivedInAnyOrder(FCREPO_URI,
                baseUrl + "/foo/a", baseUrl + "/foo/b", baseUrl + "/foo/c", baseUrl + "/foo/d", baseUrl + "/foo/e",
                baseUrl + "/foo/f", baseUrl + "/foo/g");

        final var context = camelContext.adapt(ModelCamelContext.class);

        AdviceWith.adviceWith(context, "FcrepoReindexingTraverse", a -> {
            a.replaceFromWith("direct:traverse");
            a.mockEndpointsAndSkip("fcrepo:*");
            a.mockEndpointsAndSkip(reindexingStream + "*");
            a.mockEndpointsAndSkip("direct:recipients");
        });

        template.sendBodyAndHeader("direct:traverse", ObjectHelper.loadResourceAsStream("indexable.nt"),
                FCREPO_URI, "http://localhost:8080/fcrepo4/rest/foo");

        MockEndpoint.assertIsSatisfied(recipientsEndpoint, reindexingEndpoint);
    }

    @DirtiesContext
    @Test
    public void testRecipientList() throws Exception {
        final String id = "/foo";


        final var context = camelContext.adapt(ModelCamelContext.class);

        AdviceWith.adviceWith(context, "FcrepoReindexingTraverse", a -> {
            a.replaceFromWith("direct:traverse");
            a.mockEndpointsAndSkip("broker:*");
            a.mockEndpointsAndSkip("fcrepo:*");
        });

        final var fooEndpoint = MockEndpoint.resolve(camelContext, "mock:foo");
        final var barEndpoint = MockEndpoint.resolve(camelContext, "mock:bar");

        fooEndpoint.expectedMessageCount(1);
        fooEndpoint.expectedHeaderReceived(FCREPO_URI, baseUrl + id);
        barEndpoint.expectedMessageCount(1);
        barEndpoint.expectedHeaderReceived(FCREPO_URI, baseUrl + id);

        final Map<String, Object> headers = new HashMap<>();
        headers.put(FCREPO_URI, baseUrl + id);
        headers.put(REINDEXING_RECIPIENTS, "mock:foo,mock:bar");

        template.sendBodyAndHeaders("direct:recipients", null, headers);

        MockEndpoint.assertIsSatisfied(fooEndpoint, barEndpoint);
    }

    @Configuration
    @ComponentScan(basePackages = {"org.fcrepo.camel"})
    static class ContextConfig extends CamelConfiguration {

        @Bean
        public RouteBuilder route() {
            return new ReindexingRouter();
        }
    }
}
