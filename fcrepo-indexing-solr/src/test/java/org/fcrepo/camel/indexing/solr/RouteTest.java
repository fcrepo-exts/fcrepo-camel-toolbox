/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel.indexing.solr;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.spring.javaconfig.CamelConfiguration;
import org.apache.commons.io.IOUtils;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.camel.util.ObjectHelper.loadResourceAsStream;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_AGENT;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_DATE_TIME;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_EVENT_TYPE;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_RESOURCE_TYPE;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;

/**
 * Test the route workflow.
 *
 * @author Aaron Coburn
 * @since 2015-04-10
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {RouteTest.ContextConfig.class}, loader = AnnotationConfigContextLoader.class)
public class RouteTest {

    private static final long ASSERT_PERIOD_MS = 5000;
    private final String EVENT_NS = "https://www.w3.org/ns/activitystreams#";
    private final String INDEXABLE = "http://fedora.info/definitions/v4/indexing#Indexable";
    private static final String baseURL = "http://localhost/rest";
    private static final String solrURL = "http:localhost:8983/solr/collection1";
    private static final String fileID = "/file1";
    private static final String eventDate = "2015-04-06T22:45:20Z";
    private static final String userID = "bypassAdmin";
    private static final String userAgent = "curl/7.37.1";
    private static final String auditContainer = "/audit";

    @Autowired
    private CamelContext camelContext;

    @Produce("direct:start")
    protected ProducerTemplate template;

    @BeforeClass
    public static void beforeClass() {

        System.setProperty("solr.indexing.enabled", "true");
        System.setProperty("solr.indexing.predicate", "true");
        System.setProperty("solr.filter.containers", baseURL + auditContainer);
        System.setProperty("solr.input.stream", "seda:foo");
        System.setProperty("solr.reindex.stream", "seda:bar");
        System.setProperty("error.maxRedeliveries", "10");
        System.setProperty("fcrepo.baseUrl", baseURL);
        System.setProperty("solr.fcrepo.defaultTransform", "http://localhost/ldpath/program");
        System.setProperty("solr.baseUrl", solrURL);
        System.setProperty("solr.reindex.stream", "seda:reindex");
        System.setProperty("solr.fcrepo.checkHasIndexingTransformation", "true");

    }

    @DirtiesContext
    @Test
    public void testEventTypeRouter() throws Exception {

        final List<String> eventTypes = asList(EVENT_NS + "Delete");

        final var context = camelContext.adapt(ModelCamelContext.class);
        AdviceWith.adviceWith(context, "FcrepoSolrRouter", a -> {
            a.replaceFromWith("direct:start");
            a.mockEndpointsAndSkip("direct:index.solr");
            a.mockEndpointsAndSkip("direct:delete.solr");
        });



        final var deleteEndpoint = MockEndpoint.resolve(camelContext, "mock:direct:delete.solr");
        final var indexEndpoint = MockEndpoint.resolve(camelContext, "mock:direct:index.solr");
        deleteEndpoint.expectedMessageCount(1);
        indexEndpoint.expectedMessageCount(0);
        indexEndpoint.setAssertPeriod(ASSERT_PERIOD_MS);
        template.sendBodyAndHeaders(loadResourceAsStream("event_delete_resource.json"),
                createEvent(baseURL + fileID, eventTypes));

        MockEndpoint.assertIsSatisfied(deleteEndpoint, indexEndpoint);
    }

    @DirtiesContext
    @Test
    public void testFilterAuditEvents() throws Exception {

        final List<String> eventTypes = asList(EVENT_NS + "ResourceCreation");

        final var context = camelContext.adapt(ModelCamelContext.class);
        AdviceWith.adviceWith(context, "FcrepoSolrIndexer", a -> {
            a.replaceFromWith("direct:start");
            a.mockEndpointsAndSkip("fcrepo*");
            a.mockEndpointsAndSkip("direct:update.solr");
            a.mockEndpointsAndSkip("direct:delete.solr");
        });

        final var deleteEndpoint = MockEndpoint.resolve(camelContext, "mock:direct:delete.solr");
        final var updateEndpoint = MockEndpoint.resolve(camelContext, "mock:direct:update.solr");
        deleteEndpoint.expectedMessageCount(0);
        deleteEndpoint.setAssertPeriod(ASSERT_PERIOD_MS);
        updateEndpoint.expectedMessageCount(0);
        updateEndpoint.setAssertPeriod(ASSERT_PERIOD_MS);


        template.sendBodyAndHeaders(
                IOUtils.toString(loadResourceAsStream("indexable.rdf"), "UTF-8"),
                createEvent(baseURL + auditContainer + fileID, eventTypes));

        MockEndpoint.assertIsSatisfied(deleteEndpoint, updateEndpoint);
    }

    @DirtiesContext
    @Test
    public void testFilterAuditExactMatch() throws Exception {

        final List<String> eventTypes = asList(EVENT_NS + "ResourceModification");
        final var context = camelContext.adapt(ModelCamelContext.class);
        AdviceWith.adviceWith(context, "FcrepoSolrIndexer", a -> {
            a.replaceFromWith("direct:start");
            a.mockEndpointsAndSkip("fcrepo*");
            a.mockEndpointsAndSkip("direct:update.solr");
            a.mockEndpointsAndSkip("direct:delete.solr");
        });

        final var deleteEndpoint = MockEndpoint.resolve(camelContext, "mock:direct:delete.solr");
        final var updateEndpoint = MockEndpoint.resolve(camelContext, "mock:direct:update.solr");
        deleteEndpoint.expectedMessageCount(0);
        updateEndpoint.expectedMessageCount(0);
        deleteEndpoint.setAssertPeriod(ASSERT_PERIOD_MS);
        updateEndpoint.setAssertPeriod(ASSERT_PERIOD_MS);

        template.sendBodyAndHeaders(
                IOUtils.toString(loadResourceAsStream("indexable.rdf"), "UTF-8"),
                createEvent(baseURL + auditContainer, eventTypes));

        MockEndpoint.assertIsSatisfied(deleteEndpoint, updateEndpoint);

    }

    @DirtiesContext
    @Test
    public void testFilterAuditNearMatch() throws Exception {

        final List<String> eventTypes = asList(EVENT_NS + "ResourceCreation");
        final var context = camelContext.adapt(ModelCamelContext.class);
        AdviceWith.adviceWith(context, "FcrepoSolrIndexer", a -> {
            a.replaceFromWith("direct:start");
            a.mockEndpointsAndSkip("fcrepo*");
            a.mockEndpointsAndSkip("direct:update.solr");
            a.mockEndpointsAndSkip("direct:delete.solr");
        });

        final var deleteEndpoint = MockEndpoint.resolve(camelContext, "mock:direct:delete.solr");
        final var updateEndpoint = MockEndpoint.resolve(camelContext, "mock:direct:update.solr");
        deleteEndpoint.expectedMessageCount(0);
        deleteEndpoint.setAssertPeriod(ASSERT_PERIOD_MS);
        updateEndpoint.expectedMessageCount(1);

        template.sendBodyAndHeaders(
                IOUtils.toString(loadResourceAsStream("indexable.rdf"), "UTF-8"),
                createEvent(baseURL + auditContainer + "orium" + fileID, eventTypes, asList(INDEXABLE)));

        MockEndpoint.assertIsSatisfied(deleteEndpoint, updateEndpoint);

    }

    @DirtiesContext
    @Test
    public void testPrepareRouterIndexable() throws Exception {

        final List<String> eventTypes = asList(EVENT_NS + "ResourceCreation");

        final var context = camelContext.adapt(ModelCamelContext.class);
        AdviceWith.adviceWith(context, "FcrepoSolrIndexer", a -> {
            a.replaceFromWith("direct:start");
            a.mockEndpointsAndSkip("fcrepo*");
            a.mockEndpointsAndSkip("direct:update.solr");
            a.mockEndpointsAndSkip("direct:delete.solr");
        });

        final var deleteEndpoint = MockEndpoint.resolve(camelContext, "mock:direct:delete.solr");
        final var updateEndpoint = MockEndpoint.resolve(camelContext, "mock:direct:update.solr");
        deleteEndpoint.expectedMessageCount(0);
        deleteEndpoint.setAssertPeriod(ASSERT_PERIOD_MS);
        updateEndpoint.expectedMessageCount(1);
        updateEndpoint.expectedHeaderReceived("CamelIndexingTransformation",
                "http://localhost/ldpath/default");

        template.sendBodyAndHeaders(
                IOUtils.toString(loadResourceAsStream("indexable.rdf"), "UTF-8"),
                createEvent(baseURL + fileID, eventTypes, asList(INDEXABLE)));

        MockEndpoint.assertIsSatisfied(deleteEndpoint, updateEndpoint);
    }

    @DirtiesContext
    @Test
    public void testPrepareRouterContainer() throws Exception {

        final List<String> eventTypes = asList(EVENT_NS + "ResourceCreation");

        final var context = camelContext.adapt(ModelCamelContext.class);
        AdviceWith.adviceWith(context, "FcrepoSolrIndexer", a -> {

            a.replaceFromWith("direct:start");
            a.mockEndpointsAndSkip("fcrepo*");
            a.mockEndpointsAndSkip("direct:update.solr");
            a.mockEndpointsAndSkip("direct:delete.solr");
        });

        final var deleteEndpoint = MockEndpoint.resolve(camelContext, "mock:direct:delete.solr");
        final var updateEndpoint = MockEndpoint.resolve(camelContext, "mock:direct:update.solr");

        updateEndpoint.expectedMessageCount(0);
        updateEndpoint.setAssertPeriod(ASSERT_PERIOD_MS);
        deleteEndpoint.expectedMessageCount(1);
        deleteEndpoint.expectedHeaderReceived("CamelIndexingTransformation",
                "http://localhost/ldpath/program");

        template.sendBodyAndHeaders(
                IOUtils.toString(loadResourceAsStream("container.rdf"), "UTF-8"),
                createEvent(baseURL + fileID, eventTypes));

        MockEndpoint.assertIsSatisfied(deleteEndpoint, updateEndpoint);
    }

    @DirtiesContext
    @Test
    public void testUpdateRouter() throws Exception {

        final List<String> eventTypes = asList(EVENT_NS + "ResourceCreation");

        final var context = camelContext.adapt(ModelCamelContext.class);

        AdviceWith.adviceWith(context, "FcrepoSolrUpdater", a -> {
                a.mockEndpointsAndSkip("fcrepo*");
                a.mockEndpointsAndSkip("http4*");
            });

        AdviceWith.adviceWith(context, "FcrepoSolrSend", a -> {
                a.mockEndpointsAndSkip("http*");
        });

        AdviceWith.adviceWith(context, "FcrepoSolrTransform", a -> {
                a.mockEndpointsAndSkip("http*");
        });

        final var solrUpdateEndPoint = MockEndpoint.resolve(context, "mock:" + solrURL + "/update");
        solrUpdateEndPoint.expectedMessageCount(1);
        solrUpdateEndPoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");

        template.sendBodyAndHeaders("direct:update.solr", "",
                createEvent(baseURL + fileID, eventTypes));

        MockEndpoint.assertIsSatisfied(solrUpdateEndPoint);
    }

    @DirtiesContext
    @Test
    public void testDeleteRouter() throws Exception {

        final List<String> eventTypes = asList(EVENT_NS + "ResourceDeletion");


        final var context = camelContext.adapt(ModelCamelContext.class);
        AdviceWith.adviceWith(context, "FcrepoSolrDeleter", a -> {
            a.mockEndpointsAndSkip("http*");
        });

        final var solrUpdateEndpoint = MockEndpoint.resolve(camelContext, "mock:" + solrURL + "/update");
        solrUpdateEndpoint.expectedMessageCount(1);
        solrUpdateEndpoint.expectedHeaderReceived(Exchange.CONTENT_TYPE, "application/json");
        solrUpdateEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");

        template.sendBodyAndHeaders("direct:delete.solr", "",
                createEvent(baseURL + fileID, eventTypes));

        MockEndpoint.assertIsSatisfied(solrUpdateEndpoint);
    }

    private static Map<String, Object> createEvent(final String identifier, final List<String> eventTypes) {
        return createEvent(identifier, eventTypes, emptyList());
    }

    private static Map<String, Object> createEvent(final String identifier, final List<String> eventTypes,
                                                   final List<String> resourceTypes) {
        final Map<String, Object> headers = new HashMap<>();
        headers.put(FCREPO_URI, identifier);
        headers.put(FCREPO_DATE_TIME, eventDate);
        headers.put(FCREPO_AGENT, asList(userID, userAgent));
        headers.put(FCREPO_EVENT_TYPE, eventTypes);
        headers.put(FCREPO_RESOURCE_TYPE, resourceTypes);
        return headers;
    }

    @Configuration
    @ComponentScan(resourcePattern = "**/Fcrepo*.class")
    static class ContextConfig extends CamelConfiguration {

        @Bean
        public RouteBuilder route() {
            return new SolrRouter();
        }
    }
}
