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
package org.fcrepo.camel.indexing.solr;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.camel.util.ObjectHelper.loadResourceAsStream;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_AGENT;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_DATE_TIME;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_EVENT_TYPE;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_RESOURCE_TYPE;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.commons.io.IOUtils;

import org.junit.Test;

/**
 * Test the route workflow.
 *
 * @author Aaron Coburn
 * @since 2015-04-10
 */
public class RouteTest extends CamelBlueprintTestSupport {

    private final String EVENT_NS = "https://www.w3.org/ns/activitystreams#";
    private final String INDEXABLE = "http://fedora.info/definitions/v4/indexing#Indexable";

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    @Override
    protected String getBlueprintDescriptor() {
        return "/OSGI-INF/blueprint/blueprint-test.xml";
    }

    private static final String baseURL = "http://localhost/rest";
    private static final String solrURL = "http4:localhost:8983/solr/collection1";
    private static final String fileID = "/file1";
    private static final String eventDate = "2015-04-06T22:45:20Z";
    private static final String userID = "bypassAdmin";
    private static final String userAgent = "curl/7.37.1";
    private static final String auditContainer = "/audit";

    @Override
    public boolean isUseAdviceWith() {
        return true;
    }

    @Override
    protected CamelContext createCamelContext() throws Exception {
        final CamelContext ctx = getOsgiService(CamelContext.class, "(camel.context.name=FcrepoSolrIndexerTest)",
                10000);
        context = (ModelCamelContext)ctx;
        return ctx;
    }

    @Override
    public boolean isUseRouteBuilder() {
        return false;
    }

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
         final Properties props = new Properties();
         props.put("indexing.predicate", "true");
         props.put("filter.containers", baseURL + auditContainer);
         props.put("input.stream", "seda:foo");
         props.put("reindex.stream", "seda:bar");
         props.put("error.maxRedeliveries", "10");
         props.put("fcrepo.baseUrl", baseURL);
         props.put("fcrepo.defaultTransform", "http://localhost/ldpath/program");
         props.put("solr.baseUrl", solrURL);
         props.put("solr.reindex.stream", "seda:reindex");
         return props;
    }

    @Test
    public void testEventTypeRouter() throws Exception {

        final List<String> eventTypes = asList(EVENT_NS + "Delete");

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

        template.sendBodyAndHeaders(loadResourceAsStream("event_delete_resource.json"),
                createEvent(baseURL + fileID, eventTypes));

        assertMockEndpointsSatisfied();
    }


    @Test
    public void testFilterAuditEvents() throws Exception {

        final List<String> eventTypes = asList(EVENT_NS + "ResourceCreation");

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
                IOUtils.toString(loadResourceAsStream("indexable.rdf"), "UTF-8"),
                createEvent(baseURL + auditContainer + fileID, eventTypes));

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testFilterAuditExactMatch() throws Exception {

        final List<String> eventTypes = asList(EVENT_NS + "ResourceModification");

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
                IOUtils.toString(loadResourceAsStream("indexable.rdf"), "UTF-8"),
                createEvent(baseURL + auditContainer, eventTypes));

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testFilterAuditNearMatch() throws Exception {

        final List<String> eventTypes = asList(EVENT_NS + "ResourceCreation");

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
                IOUtils.toString(loadResourceAsStream("indexable.rdf"), "UTF-8"),
                createEvent(baseURL + auditContainer + "orium" + fileID, eventTypes, asList(INDEXABLE)));

        assertMockEndpointsSatisfied();
    }


    @Test
    public void testPrepareRouterIndexable() throws Exception {

        final List<String> eventTypes = asList(EVENT_NS + "ResourceCreation");

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
        getMockEndpoint("mock:direct:update.solr").expectedHeaderReceived("CamelIndexingTransformation",
                "http://localhost/ldpath/default");
        getMockEndpoint("mock:direct:delete.solr").expectedMessageCount(0);

        template.sendBodyAndHeaders(
                IOUtils.toString(loadResourceAsStream("indexable.rdf"), "UTF-8"),
                createEvent(baseURL + fileID, eventTypes, asList(INDEXABLE)));

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testPrepareRouterContainer() throws Exception {

        final List<String> eventTypes = asList(EVENT_NS + "ResourceCreation");

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
        getMockEndpoint("mock:direct:delete.solr").expectedHeaderReceived("CamelIndexingTransformation",
                "http://localhost/ldpath/program");

        template.sendBodyAndHeaders(
                IOUtils.toString(loadResourceAsStream("container.rdf"), "UTF-8"),
                createEvent(baseURL + fileID, eventTypes));

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testUpdateRouter() throws Exception {

        final List<String> eventTypes = asList(EVENT_NS + "ResourceCreation");

        context.getRouteDefinition("FcrepoSolrUpdater").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                mockEndpointsAndSkip("fcrepo*");
                mockEndpointsAndSkip("http4*");
            }
        });

        context.getRouteDefinition("FcrepoSolrSend").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                mockEndpointsAndSkip("http4*");
            }
        });

        context.getRouteDefinition("FcrepoSolrTransform").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                mockEndpointsAndSkip("http4*");
            }
        });
        context.start();

        getMockEndpoint("mock:" + solrURL + "/update").expectedMessageCount(1);
        getMockEndpoint("mock:" + solrURL + "/update")
            .expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");

        template.sendBodyAndHeaders("direct:update.solr", "",
                createEvent(baseURL + fileID, eventTypes));

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testDeleteRouter() throws Exception {

        final List<String> eventTypes = asList(EVENT_NS + "ResourceDeletion");

        context.getRouteDefinition("FcrepoSolrDeleter").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                mockEndpointsAndSkip("http4*");
            }
        });
        context.start();

        getMockEndpoint("mock:" + solrURL + "/update").expectedMessageCount(1);
        getMockEndpoint("mock:" + solrURL + "/update")
            .expectedHeaderReceived(Exchange.CONTENT_TYPE, "application/json");
        getMockEndpoint("mock:" + solrURL + "/update")
            .expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");

        template.sendBodyAndHeaders("direct:delete.solr", "",
                createEvent(baseURL + fileID, eventTypes));

        assertMockEndpointsSatisfied();
    }

    private static Map<String,Object> createEvent(final String identifier, final List<String> eventTypes) {
        return createEvent(identifier, eventTypes, emptyList());
    }

    private static Map<String,Object> createEvent(final String identifier, final List<String> eventTypes,
            final List<String> resourceTypes) {
        final Map<String, Object> headers = new HashMap<>();
        headers.put(FCREPO_URI, identifier);
        headers.put(FCREPO_DATE_TIME, eventDate);
        headers.put(FCREPO_AGENT, asList(userID, userAgent));
        headers.put(FCREPO_EVENT_TYPE, eventTypes);
        headers.put(FCREPO_RESOURCE_TYPE, resourceTypes);
        return headers;
    }
}
