/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel.indexing.triplestore.integration;

import org.apache.camel.CamelContext;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ModelCamelContext;
import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.net.URI;
import java.util.Properties;

import static com.jayway.awaitility.Awaitility.await;
import static java.lang.Integer.parseInt;
import static org.apache.camel.util.ObjectHelper.loadResourceAsStream;
import static org.fcrepo.camel.indexing.triplestore.integration.TestUtils.ASSERT_PERIOD_MS;
import static org.fcrepo.camel.indexing.triplestore.integration.TestUtils.createFcrepoClient;
import static org.fcrepo.camel.indexing.triplestore.integration.TestUtils.getEvent;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Test the route workflow.
 *
 * @author Aaron Coburn
 * @since 2015-04-10
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {RouteUpdateIT.ContextConfig.class}, loader = AnnotationConfigContextLoader.class)
public class RouteDeleteIT {

    final private Logger logger = getLogger(RouteDeleteIT.class);

    private static FusekiServer server = null;

    private static final String AS_NS = "https://www.w3.org/ns/activitystreams#";

    private String fullPath = "";

    private static final String FUSEKI_PORT = System.getProperty(
        "fuseki.dynamic.test.port", "8080"
    );

    private static final String FCREPO_PORT = System.getProperty(
        "fcrepo.dynamic.test.port", "8080"
    );

    @Produce("direct:start")
    protected ProducerTemplate template;

    @Autowired
    private CamelContext camelContext;

    @BeforeClass
    public static void beforeClass() {
        final String jmsPort = System.getProperty("fcrepo.dynamic.jms.port", "61616");
        final Properties props = new Properties();
        System.setProperty("triplestore.indexing.enabled", "true");
        System.setProperty("triplestore.indexing.predicate", "true");
        System.setProperty("triplestore.baseUrl", "http://localhost:" + FUSEKI_PORT + "/fuseki/test/update");
        System.setProperty("fcrepo.baseUrl", "http://localhost:" + FCREPO_PORT + "/fcrepo/rest");
        System.setProperty("jms.brokerUrl", "tcp://localhost:" + jmsPort);
        System.setProperty("triplestore.input.stream", "direct:start");
    }

    @After
    public void tearDownFuseki() throws Exception {
        logger.info("Stopping EmbeddedFusekiServer");
        server.stop();
    }

    @Before
    public void setUpFuseki() throws Exception {
        final FcrepoClient client = createFcrepoClient();
        final FcrepoResponse res = client.post(URI.create("http://localhost:" + FCREPO_PORT + "/fcrepo/rest"))
            .body(loadResourceAsStream("container.ttl"), "text/turtle").perform();
        fullPath = res.getLocation().toString();

        logger.info("Starting EmbeddedFusekiServer on port {}", FUSEKI_PORT);
        final Dataset ds = DatasetFactory.createTxnMem(); //new DatasetImpl(createDefaultModel());
        server = FusekiServer.create()
            .verbose(true)
            .port(parseInt(FUSEKI_PORT))
            .contextPath("/fuseki")
            .add("/test", ds, true)
            .build();
        server.start();
    }

    @DirtiesContext
    @Test
    public void testDeletedResourceWithEventBody() throws Exception {
        final String path = fullPath.replaceFirst("http://localhost:[0-9]+/fcrepo/rest", "");
        final String fusekiEndpoint = "mock:http:localhost:" + FUSEKI_PORT + "/fuseki/test/update";
        final String fcrepoEndpoint = "mock:fcrepo:http://localhost:" + FCREPO_PORT + "/fcrepo/rest";
        final String fusekiBase = "http://localhost:" + FUSEKI_PORT + "/fuseki/test";

        final var context = camelContext.adapt(ModelCamelContext.class);

        AdviceWith.adviceWith(context, "FcrepoTriplestoreRouter", a -> {
            a.mockEndpoints("*");
        });

        AdviceWith.adviceWith(context, "FcrepoTriplestoreUpdater", a -> {
            a.mockEndpoints("*");
        });

        AdviceWith.adviceWith(context, "FcrepoTriplestoreDeleter", a -> {
            a.mockEndpoints("*");
        });

        TestUtils.populateFuseki(fusekiBase, fullPath);

        await().until(TestUtils.triplestoreCount(fusekiBase, fullPath), greaterThanOrEqualTo(1));

        final var fusekiMockEndpoint = MockEndpoint.resolve(camelContext, fusekiEndpoint);
        fusekiMockEndpoint.expectedMessageCount(1);

        final var deleteEndpoint = MockEndpoint.resolve(camelContext, "mock://direct:delete.triplestore");
        deleteEndpoint.expectedMessageCount(1);
        final var updateEndpoint = MockEndpoint.resolve(camelContext, "mock://direct:update.triplestore");
        updateEndpoint.expectedMessageCount(0);
        updateEndpoint.setAssertPeriod(ASSERT_PERIOD_MS);
        final var fcrepoMockEndpoint = MockEndpoint.resolve(camelContext, fcrepoEndpoint);
        fcrepoMockEndpoint.expectedMessageCount(0);
        fcrepoMockEndpoint.setAssertPeriod(ASSERT_PERIOD_MS);
        template.sendBody("direct:start", getEvent(fullPath, AS_NS + "Delete"));

        await().until(TestUtils.triplestoreCount(fusekiBase, fullPath), equalTo(0));

        MockEndpoint.assertIsSatisfied(fusekiMockEndpoint, fcrepoMockEndpoint, deleteEndpoint, updateEndpoint);
    }
}
