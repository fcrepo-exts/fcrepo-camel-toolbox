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
package org.fcrepo.camel.indexing.triplestore;

import static java.net.URLEncoder.encode;
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

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.commons.io.IOUtils;

import org.junit.Test;

/**
 * Test the route workflow.
 *
 * @author Aaron Coburn
 * @since 2015-04-22
 */
public class RouteTest extends CamelBlueprintTestSupport {

    private static final String REPOSITORY = "http://fedora.info/definitions/v4/repository#";

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    private static final String baseURL = "http://localhost/rest";
    private static final String fileID = "/file1";
    private static final long timestamp = 1428360320168L;
    private static final String eventDate = "2015-04-06T22:45:20Z";
    private static final String userID = "bypassAdmin";
    private static final String userAgent = "curl/7.37.1";
    private static final String auditContainer = "/audit";
    private static final String AS_NS = "https://www.w3.org/ns/activitystreams#";
    private static final String INDEXABLE = "http://fedora.info/definitions/v4/indexing#Indexable";

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
         props.put("indexing.predicate", "true");
         props.put("filter.containers", auditContainer);
         props.put("input.stream", "seda:foo");
         props.put("reindex.stream", "seda:bar");
         props.put("triplestore.reindex.stream", "seda:reindex");
         return props;
    }

    @Test
    public void testEventTypeRouter() throws Exception {

        context.getRouteDefinition("FcrepoTriplestoreRouter").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpointsAndSkip("*");
            }
        });
        context.start();

        getMockEndpoint("mock:direct:delete.triplestore").expectedMessageCount(1);
        getMockEndpoint("mock:direct:index.triplestore").expectedMessageCount(0);

        template.sendBody(
                IOUtils.toString(loadResourceAsStream("event_delete_resource.json"), "UTF-8"));

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testAuditFilter() throws Exception {

        context.getRouteDefinition("FcrepoTriplestoreIndexer").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpointsAndSkip("*");
            }
        });
        context.start();

        getMockEndpoint("mock:direct:delete.triplestore").expectedMessageCount(0);
        getMockEndpoint("mock:direct:update.triplestore").expectedMessageCount(0);

        template.sendBodyAndHeaders("",
                createEvent(auditContainer + fileID, asList(AS_NS + "Update"), asList(REPOSITORY + "Binary")));
        template.sendBodyAndHeaders("",
                createEvent(auditContainer + fileID, asList(AS_NS + "Delete"), asList(REPOSITORY + "Binary")));

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testAuditFilterExactMatch() throws Exception {

        context.getRouteDefinition("FcrepoTriplestoreIndexer").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpointsAndSkip("*");
            }
        });
        context.start();

        getMockEndpoint("mock:direct:delete.triplestore").expectedMessageCount(0);
        getMockEndpoint("mock:direct:update.triplestore").expectedMessageCount(0);

        template.sendBodyAndHeaders("",
                createEvent(auditContainer, asList(AS_NS + "Update"), asList(REPOSITORY + "Binary")));
        template.sendBodyAndHeaders("",
                createEvent(auditContainer, asList(AS_NS + "Delete"), asList(REPOSITORY + "Binary")));

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testAuditFilterNearMatch() throws Exception {

        context.getRouteDefinition("FcrepoTriplestoreIndexer").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpointsAndSkip("*");
            }
        });
        context.start();

        getMockEndpoint("mock:direct:delete.triplestore").expectedMessageCount(1);
        getMockEndpoint("mock:direct:update.triplestore").expectedMessageCount(0);

        template.sendBodyAndHeaders(
                IOUtils.toString(loadResourceAsStream("container.rdf"), "UTF-8"),
                createEvent(auditContainer + "orium" + fileID,
                    asList(AS_NS + "Create"), asList(REPOSITORY + "Resource")));

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testAuditFilterNearMatchIndexable() throws Exception {

        context.getRouteDefinition("FcrepoTriplestoreIndexer").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpointsAndSkip("*");
            }
        });
        context.start();

        getMockEndpoint("mock:direct:delete.triplestore").expectedMessageCount(0);
        getMockEndpoint("mock:direct:update.triplestore").expectedMessageCount(1);

        template.sendBodyAndHeaders(
                IOUtils.toString(loadResourceAsStream("indexable.rdf"), "UTF-8"),
                createEvent(auditContainer + "orium" + fileID,
                    asList(AS_NS + "Create"), asList(REPOSITORY + "Container")));

        assertMockEndpointsSatisfied();
    }


    @Test
    public void testPrepareRouterIndexable() throws Exception {

        context.getRouteDefinition("FcrepoTriplestoreRouter").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpointsAndSkip("fcrepo*");
                mockEndpointsAndSkip("direct:index.triplestore");
                mockEndpointsAndSkip("direct:delete.triplestore");
            }
        });

        context.start();

        getMockEndpoint("mock:direct:index.triplestore").expectedMessageCount(1);
        getMockEndpoint("mock:direct:delete.triplestore").expectedMessageCount(0);

        template.sendBodyAndHeaders(
                IOUtils.toString(loadResourceAsStream("event.json"), "UTF-8"),
                createEvent(fileID, asList(AS_NS + "Create"), asList(INDEXABLE)));

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testIndexRouterContainer() throws Exception {

        context.getRouteDefinition("FcrepoTriplestoreIndexer").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpointsAndSkip("fcrepo*");
                mockEndpointsAndSkip("direct:update.triplestore");
                mockEndpointsAndSkip("direct:delete.triplestore");
            }
        });

        context.start();

        getMockEndpoint("mock:direct:update.triplestore").expectedMessageCount(0);
        getMockEndpoint("mock:direct:delete.triplestore").expectedMessageCount(1);

        template.sendBodyAndHeaders(
                IOUtils.toString(loadResourceAsStream("container.rdf"), "UTF-8"),
                createEvent(fileID, asList(AS_NS + "Create"), asList(REPOSITORY + "Container")));

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testIndexRouterIndexable() throws Exception {

        context.getRouteDefinition("FcrepoTriplestoreIndexer").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpointsAndSkip("fcrepo*");
                mockEndpointsAndSkip("direct:update.triplestore");
                mockEndpointsAndSkip("direct:delete.triplestore");
            }
        });

        context.start();

        getMockEndpoint("mock:direct:update.triplestore").expectedMessageCount(1);
        getMockEndpoint("mock:direct:delete.triplestore").expectedMessageCount(0);

        template.sendBodyAndHeaders(
                IOUtils.toString(loadResourceAsStream("indexable.rdf"), "UTF-8"),
                createEvent(fileID, asList(AS_NS + "Create"), asList(INDEXABLE)));

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testUpdateRouter() throws Exception {

        final String document = IOUtils.toString(loadResourceAsStream("container.nt"), "UTF-8").trim();
        final String responsePrefix =
                  "DELETE WHERE { <" + baseURL + fileID + "> ?p ?o };\n" +
                  "INSERT DATA { ";

        context.getRouteDefinition("FcrepoTriplestoreUpdater").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                mockEndpointsAndSkip("fcrepo*");
                mockEndpointsAndSkip("http*");
            }
        });

        context.start();

        final MockEndpoint endpoint = getMockEndpoint("mock:http:localhost:8080/fuseki/test/update");

        endpoint.expectedMessageCount(1);
        endpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");
        endpoint.expectedHeaderReceived(Exchange.CONTENT_TYPE, "application/x-www-form-urlencoded; charset=utf-8");
        endpoint.allMessages().body().startsWith("update=" + encode(responsePrefix, "UTF-8"));
        endpoint.allMessages().body().endsWith(encode("\n}", "UTF-8"));
        for (final String s : document.split("\n")) {
            endpoint.expectedBodyReceived().body().contains(encode(s, "UTF-8"));
        }

        final Map<String, Object> headers = createEvent(baseURL + fileID, asList(AS_NS + "Create"),
                    asList(REPOSITORY + "Container"));
        headers.put(Exchange.CONTENT_TYPE, "application/rdf+xml");

        template.sendBodyAndHeaders("direct:update.triplestore",
                IOUtils.toString(loadResourceAsStream("container.rdf"), "UTF-8"),
                headers);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testDeleteRouter() throws Exception {

        final String eventTypes = REPOSITORY + "NODE_REMOVED";
        final String eventProps = REPOSITORY + "hasContent";

        context.getRouteDefinition("FcrepoTriplestoreDeleter").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                mockEndpointsAndSkip("http*");
            }
        });

        context.start();

        getMockEndpoint("mock:http:localhost:8080/fuseki/test/update").expectedMessageCount(1);
        getMockEndpoint("mock:http:localhost:8080/fuseki/test/update")
            .expectedHeaderReceived(Exchange.CONTENT_TYPE, "application/x-www-form-urlencoded; charset=utf-8");
        getMockEndpoint("mock:http:localhost:8080/fuseki/test/update")
            .expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");

        template.sendBodyAndHeaders("direct:delete.triplestore", "",
                createEvent(fileID, asList(AS_NS + "Delete")));

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
