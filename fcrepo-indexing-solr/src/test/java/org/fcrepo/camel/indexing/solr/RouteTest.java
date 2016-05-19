/*
 * Copyright 2016 DuraSpace, Inc.
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
package org.fcrepo.camel.indexing.solr;

import static org.fcrepo.camel.FcrepoHeaders.FCREPO_TRANSFORM;
import static org.fcrepo.camel.RdfNamespaces.REPOSITORY;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.util.ObjectHelper;
import org.apache.commons.io.IOUtils;
import org.fcrepo.camel.JmsHeaders;

import org.junit.Test;

/**
 * Test the route workflow.
 *
 * @author Aaron Coburn
 * @since 2015-04-10
 */
public class RouteTest extends CamelTestSupport {

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    private static final String baseURL = "http://localhost/rest";
    private static final String solrURL = "localhost:8983/solr/collection1";
    private static final String fileID = "/file1";
    private static final long timestamp = 1428360320168L;
    private static final String eventDate = "2015-04-06T22:45:20Z";
    private static final String userID = "bypassAdmin";
    private static final String userAgent = "curl/7.37.1";
    private static final String auditContainer = "/audit";

    @Override
    public boolean isUseAdviceWith() {
        return true;
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new SolrRouter();
    }

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
         final Properties props = new Properties();
         props.put("indexing.predicate", "true");
         props.put("audit.container", auditContainer);
         props.put("input.stream", "seda:foo");
         props.put("reindex.stream", "seda:bar");
         props.put("error.maxRedeliveries", "10");
         props.put("fcrepo.baseUrl", baseURL);
         props.put("fcrepo.defaultTransform", "default");
         props.put("solr.baseUrl", solrURL);
         props.put("solr.commitWithin", "100");
         props.put("solr.reindex.stream", "seda:reindex");
         return props;
    }


    @Test
    public void testEventTypeRouter() throws Exception {

        final String eventTypes = REPOSITORY + "NODE_REMOVED";
        final String eventProps = REPOSITORY + "hasContent";

        context.getRouteDefinition("FcrepoSolrRouter").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpointsAndSkip("direct:index.solr");
                mockEndpointsAndSkip("direct:delete.solr");
            }
        });
        context.start();

        getMockEndpoint("mock:direct:delete.solr").expectedMessageCount(1);
        getMockEndpoint("mock:direct:index.solr").expectedMessageCount(0);

        template.sendBodyAndHeaders("",
                createEvent(fileID, eventTypes, eventProps));

        assertMockEndpointsSatisfied();
    }


    @Test
    public void testFilterAuditEvents() throws Exception {

        final String eventTypes = REPOSITORY + "NODE_REMOVED";
        final String eventProps = REPOSITORY + "hasContent";

        context.getRouteDefinition("FcrepoSolrIndexer").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpointsAndSkip("fcrepo*");
                mockEndpointsAndSkip("direct:update.solr");
                mockEndpointsAndSkip("direct:delete.solr");
            }
        });
        context.start();

        getMockEndpoint("mock:direct:delete.solr").expectedMessageCount(0);
        getMockEndpoint("mock:direct:update.solr").expectedMessageCount(0);

        template.sendBodyAndHeaders(
                IOUtils.toString(ObjectHelper.loadResourceAsStream("indexable.rdf"), "UTF-8"),
                createEvent(auditContainer + fileID, eventTypes, eventProps));

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testFilterAuditExactMatch() throws Exception {

        final String eventTypes = REPOSITORY + "NODE_REMOVED";
        final String eventProps = REPOSITORY + "hasContent";

        context.getRouteDefinition("FcrepoSolrIndexer").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpointsAndSkip("fcrepo*");
                mockEndpointsAndSkip("direct:update.solr");
                mockEndpointsAndSkip("direct:delete.solr");
            }
        });
        context.start();

        getMockEndpoint("mock:direct:delete.solr").expectedMessageCount(0);
        getMockEndpoint("mock:direct:update.solr").expectedMessageCount(0);

        template.sendBodyAndHeaders(
                IOUtils.toString(ObjectHelper.loadResourceAsStream("indexable.rdf"), "UTF-8"),
                createEvent(auditContainer, eventTypes, eventProps));

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testFilterAuditNearMatch() throws Exception {

        final String eventTypes = REPOSITORY + "NODE_ADDED";
        final String eventProps = REPOSITORY + "hasContent";

        context.getRouteDefinition("FcrepoSolrIndexer").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpointsAndSkip("fcrepo*");
                mockEndpointsAndSkip("direct:update.solr");
                mockEndpointsAndSkip("direct:delete.solr");
            }
        });
        context.start();

        getMockEndpoint("mock:direct:delete.solr").expectedMessageCount(0);
        getMockEndpoint("mock:direct:update.solr").expectedMessageCount(1);

        template.sendBodyAndHeaders(
                IOUtils.toString(ObjectHelper.loadResourceAsStream("indexable.rdf"), "UTF-8"),
                createEvent(auditContainer + "orium" + fileID, eventTypes, eventProps));

        assertMockEndpointsSatisfied();
    }


    @Test
    public void testPrepareRouterIndexable() throws Exception {

        final String eventTypes = REPOSITORY + "NODE_ADDED";
        final String eventProps = REPOSITORY + "hasContent";

        context.getRouteDefinition("FcrepoSolrIndexer").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpointsAndSkip("fcrepo*");
                mockEndpointsAndSkip("direct:update.solr");
                mockEndpointsAndSkip("direct:delete.solr");
            }
        });
        context.start();

        getMockEndpoint("mock:direct:update.solr").expectedMessageCount(1);
        getMockEndpoint("mock:direct:update.solr").expectedHeaderReceived(FCREPO_TRANSFORM, "default");
        getMockEndpoint("mock:direct:delete.solr").expectedMessageCount(0);

        template.sendBodyAndHeaders(
                IOUtils.toString(ObjectHelper.loadResourceAsStream("indexable.rdf"), "UTF-8"),
                createEvent(fileID, eventTypes, eventProps));

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testPrepareRouterContainer() throws Exception {

        final String eventTypes = REPOSITORY + "NODE_ADDED";
        final String eventProps = REPOSITORY + "hasContent";

        context.getRouteDefinition("FcrepoSolrIndexer").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpointsAndSkip("fcrepo*");
                mockEndpointsAndSkip("direct:update.solr");
                mockEndpointsAndSkip("direct:delete.solr");
            }
        });
        context.start();

        getMockEndpoint("mock:direct:update.solr").expectedMessageCount(0);
        getMockEndpoint("mock:direct:delete.solr").expectedMessageCount(1);
        getMockEndpoint("mock:direct:delete.solr").expectedHeaderReceived(FCREPO_TRANSFORM, "");

        template.sendBodyAndHeaders(
                IOUtils.toString(ObjectHelper.loadResourceAsStream("container.rdf"), "UTF-8"),
                createEvent(fileID, eventTypes, eventProps));

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testUpdateRouter() throws Exception {

        final String body = String.format("{\"delete\":{\"id\":\"${headers[%s]}\", \"commitWithin\": 500}}", fileID);
        final String eventTypes = REPOSITORY + "NODE_ADDED";
        final String eventProps = REPOSITORY + "hasContent";

        context.getRouteDefinition("FcrepoSolrUpdater").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                mockEndpointsAndSkip("fcrepo*");
                mockEndpointsAndSkip("http4*");
            }
        });
        context.start();

        getMockEndpoint("mock:http4:" + solrURL + "/update").expectedMessageCount(1);
        getMockEndpoint("mock:http4:" + solrURL + "/update")
            .expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");
        getMockEndpoint("mock:http4:" + solrURL + "/update").expectedBodiesReceived(body);

        template.sendBodyAndHeaders("direct:update.solr", body,
                createEvent(fileID, eventTypes, eventProps));

        assertMockEndpointsSatisfied();
    }


    @Test
    public void testDeleteRouter() throws Exception {

        final String eventTypes = REPOSITORY + "NODE_ADDED";
        final String eventProps = REPOSITORY + "hasContent";

        context.getRouteDefinition("FcrepoSolrDeleter").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                mockEndpointsAndSkip("http4*");
            }
        });
        context.start();

        getMockEndpoint("mock:http4:" + solrURL + "/update").expectedMessageCount(1);
        getMockEndpoint("mock:http4:" + solrURL + "/update")
            .expectedHeaderReceived(Exchange.CONTENT_TYPE, "application/json");
        getMockEndpoint("mock:http4:" + solrURL + "/update")
            .expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");

        template.sendBodyAndHeaders("direct:delete.solr", "",
                createEvent(fileID, eventTypes, eventProps));

        assertMockEndpointsSatisfied();
    }

    private static Map<String,Object> createEvent(final String identifier, final String eventTypes,
            final String eventProperties) {

        final Map<String, Object> headers = new HashMap<>();
        headers.put(JmsHeaders.BASE_URL, baseURL);
        headers.put(JmsHeaders.IDENTIFIER, identifier);
        headers.put(JmsHeaders.TIMESTAMP, timestamp);
        headers.put(JmsHeaders.USER, userID);
        headers.put(JmsHeaders.USER_AGENT, userAgent);
        headers.put(JmsHeaders.EVENT_TYPE, eventTypes);
        headers.put(JmsHeaders.PROPERTIES, eventProperties);
        return headers;
    }
}
