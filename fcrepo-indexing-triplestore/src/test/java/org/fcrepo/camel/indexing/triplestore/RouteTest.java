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

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.activemq.ActiveMQComponent;
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

import static java.net.URLEncoder.encode;
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
 * @since 2015-04-22
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {RouteTest.ContextConfig.class}, loader = AnnotationConfigContextLoader.class)
public class RouteTest {

    private static final String REPOSITORY = "http://fedora.info/definitions/v4/repository#";

    @EndpointInject("mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce("direct:start")
    protected ProducerTemplate template;

    @Autowired
    private CamelContext camelContext;


    private static final String baseURL = "http://localhost/rest";
    private static final String fileID = "/file1";
    private static final long timestamp = 1428360320168L;
    private static final String eventDate = "2015-04-06T22:45:20Z";
    private static final String userID = "bypassAdmin";
    private static final String userAgent = "curl/7.37.1";
    private static final String auditContainer = "/audit";
    private static final String AS_NS = "https://www.w3.org/ns/activitystreams#";
    private static final String INDEXABLE = "http://fedora.info/definitions/v4/indexing#Indexable";


    @BeforeClass
    public static void beforeClass() {
        System.setProperty("triplestore.indexing.predicate", "true");
        System.setProperty("triplestore.filter.containers", auditContainer);
        System.setProperty("triplestore.input.stream", "seda:foo");
        System.setProperty("triplestore.reindex.stream", "seda:reindex");

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

    @DirtiesContext
    @Test
    public void testEventTypeRouter() throws Exception {

        final var context = camelContext.adapt(ModelCamelContext.class);

        AdviceWith.adviceWith(context, "FcrepoTriplestoreRouter", a -> {
            a.replaceFromWith("direct:start");
            a.mockEndpointsAndSkip("direct:index.triplestore");
            a.mockEndpointsAndSkip("direct:delete.triplestore");
        });

        final var deleteEndpoint = MockEndpoint.resolve(camelContext, "mock:direct:delete.triplestore");
        final var indexEndpoint = MockEndpoint.resolve(camelContext, "mock:direct:index.triplestore");
        deleteEndpoint.expectedMessageCount(1);
        indexEndpoint.expectedMessageCount(0);

        template.sendBody(
                IOUtils.toString(loadResourceAsStream("event_delete_resource.json"), "UTF-8"));

        MockEndpoint.assertIsSatisfied(deleteEndpoint, indexEndpoint);
    }

    @DirtiesContext
    @Test
    public void testAuditFilter() throws Exception {

        final var context = camelContext.adapt(ModelCamelContext.class);

        AdviceWith.adviceWith(context, "FcrepoTriplestoreIndexer", a -> {
            a.replaceFromWith("direct:start");
            a.mockEndpointsAndSkip("fcrepo:*");
            a.mockEndpointsAndSkip("http:*");
            a.mockEndpointsAndSkip("direct:delete.triplestore");
            a.mockEndpointsAndSkip("direct:index.triplestore");
        });


        final var deleteEndpoint = MockEndpoint.resolve(camelContext, "mock:direct:delete.triplestore");
        final var updateEndpoint = MockEndpoint.resolve(camelContext, "mock:direct:update.triplestore");
        deleteEndpoint.expectedMessageCount(0);
        updateEndpoint.expectedMessageCount(0);

        template.sendBodyAndHeaders("",
                createEvent(auditContainer + fileID, asList(AS_NS + "Update"), asList(REPOSITORY + "Binary")));
        template.sendBodyAndHeaders("",
                createEvent(auditContainer + fileID, asList(AS_NS + "Delete"), asList(REPOSITORY + "Binary")));

        MockEndpoint.assertIsSatisfied(deleteEndpoint, updateEndpoint);
    }

    @DirtiesContext
    @Test
    public void testAuditFilterExactMatch() throws Exception {
        final var context = camelContext.adapt(ModelCamelContext.class);

        AdviceWith.adviceWith(context, "FcrepoTriplestoreIndexer", a -> {
            a.replaceFromWith("direct:start");
            a.mockEndpointsAndSkip("fcrepo:*");
            a.mockEndpointsAndSkip("http:*");
            a.mockEndpointsAndSkip("direct:delete.triplestore");
            a.mockEndpointsAndSkip("direct:index.triplestore");
        });

        final var deleteEndpoint = MockEndpoint.resolve(camelContext, "mock:direct:delete.triplestore");
        final var updateEndpoint = MockEndpoint.resolve(camelContext, "mock:direct:update.triplestore");
        deleteEndpoint.expectedMessageCount(0);
        updateEndpoint.expectedMessageCount(0);


        template.sendBodyAndHeaders("",
                createEvent(auditContainer, asList(AS_NS + "Update"), asList(REPOSITORY + "Binary")));
        template.sendBodyAndHeaders("",
                createEvent(auditContainer, asList(AS_NS + "Delete"), asList(REPOSITORY + "Binary")));

        MockEndpoint.assertIsSatisfied(deleteEndpoint, updateEndpoint);
    }

    @DirtiesContext
    @Test
    public void testAuditFilterNearMatch() throws Exception {
        final var context = camelContext.adapt(ModelCamelContext.class);

        AdviceWith.adviceWith(context, "FcrepoTriplestoreIndexer", a -> {
            a.replaceFromWith("direct:start");
            a.mockEndpointsAndSkip("fcrepo:*");
            a.mockEndpointsAndSkip("http:*");
            a.mockEndpointsAndSkip("direct:delete.triplestore");
            a.mockEndpointsAndSkip("direct:index.triplestore");
        });
        final var deleteEndpoint = MockEndpoint.resolve(camelContext, "mock:direct:delete.triplestore");
        final var updateEndpoint = MockEndpoint.resolve(camelContext, "mock:direct:update.triplestore");
        deleteEndpoint.expectedMessageCount(1);
        updateEndpoint.expectedMessageCount(0);


        template.sendBodyAndHeaders(
                IOUtils.toString(loadResourceAsStream("container.rdf"), "UTF-8"),
                createEvent(auditContainer + "orium" + fileID,
                        asList(AS_NS + "Create"), asList(REPOSITORY + "Resource")));

        MockEndpoint.assertIsSatisfied(deleteEndpoint, updateEndpoint);
    }

    @DirtiesContext
    @Test
    public void testAuditFilterNearMatchIndexable() throws Exception {
        final var context = camelContext.adapt(ModelCamelContext.class);

        AdviceWith.adviceWith(context, "FcrepoTriplestoreIndexer", a -> {
            a.replaceFromWith("direct:start");
            a.mockEndpointsAndSkip("fcrepo:*");
            a.mockEndpointsAndSkip("direct:delete.triplestore");
            a.mockEndpointsAndSkip("direct:update.triplestore");
        });

        final var deleteEndpoint = MockEndpoint.resolve(camelContext, "mock:direct:delete.triplestore");
        final var updateEndpoint = MockEndpoint.resolve(camelContext, "mock:direct:update.triplestore");
        deleteEndpoint.expectedMessageCount(0);
        updateEndpoint.expectedMessageCount(1);

        template.sendBodyAndHeaders(
                IOUtils.toString(loadResourceAsStream("indexable.rdf"), "UTF-8"),
                createEvent(auditContainer + "orium" + fileID,
                        asList(AS_NS + "Create"), asList(REPOSITORY + "Container")));

        MockEndpoint.assertIsSatisfied(deleteEndpoint, updateEndpoint);
    }

    @DirtiesContext
    @Test
    public void testPrepareRouterIndexable() throws Exception {
        final var context = camelContext.adapt(ModelCamelContext.class);

        AdviceWith.adviceWith(context, "FcrepoTriplestoreRouter", a -> {
            a.replaceFromWith("direct:start");
            a.mockEndpointsAndSkip("fcrepo*");
            a.mockEndpointsAndSkip("direct:index.triplestore");
            a.mockEndpointsAndSkip("direct:delete.triplestore");
        });

        final var indexEndpoint = MockEndpoint.resolve(camelContext, "mock:direct:index.triplestore");
        final var deleteEndpoint = MockEndpoint.resolve(camelContext, "mock:direct:delete.triplestore");
        indexEndpoint.expectedMessageCount(1);
        deleteEndpoint.expectedMessageCount(0);

        template.sendBodyAndHeaders(
                IOUtils.toString(loadResourceAsStream("event.json"), "UTF-8"),
                createEvent(fileID, asList(AS_NS + "Create"), asList(INDEXABLE)));

        MockEndpoint.assertIsSatisfied(deleteEndpoint, indexEndpoint);
    }

    @DirtiesContext
    @Test
    public void testIndexRouterContainer() throws Exception {
        final var context = camelContext.adapt(ModelCamelContext.class);

        AdviceWith.adviceWith(context, "FcrepoTriplestoreIndexer", a -> {
            a.replaceFromWith("direct:start");
            a.mockEndpointsAndSkip("fcrepo*");
            a.mockEndpointsAndSkip("direct:update.triplestore");
            a.mockEndpointsAndSkip("direct:delete.triplestore");
        });


        final var updateEndpoint = MockEndpoint.resolve(camelContext, "mock:direct:update.triplestore");
        final var deleteEndpoint = MockEndpoint.resolve(camelContext, "mock:direct:delete.triplestore");
        updateEndpoint.expectedMessageCount(0);
        deleteEndpoint.expectedMessageCount(1);

        template.sendBodyAndHeaders(
                IOUtils.toString(loadResourceAsStream("container.rdf"), "UTF-8"),
                createEvent(fileID, asList(AS_NS + "Create"), asList(REPOSITORY + "Container")));

        MockEndpoint.assertIsSatisfied(deleteEndpoint, updateEndpoint);
    }

    @DirtiesContext
    @Test
    public void testIndexRouterIndexable() throws Exception {
        final var context = camelContext.adapt(ModelCamelContext.class);

        AdviceWith.adviceWith(context, "FcrepoTriplestoreIndexer", a -> {
            a.replaceFromWith("direct:start");
            a.mockEndpointsAndSkip("fcrepo*");
            a.mockEndpointsAndSkip("direct:update.triplestore");
            a.mockEndpointsAndSkip("direct:delete.triplestore");
        });

        final var updateEndpoint = MockEndpoint.resolve(camelContext, "mock:direct:update.triplestore");
        final var deleteEndpoint = MockEndpoint.resolve(camelContext, "mock:direct:delete.triplestore");
        updateEndpoint.expectedMessageCount(1);
        deleteEndpoint.expectedMessageCount(0);

        template.sendBodyAndHeaders(
                IOUtils.toString(loadResourceAsStream("indexable.rdf"), "UTF-8"),
                createEvent(fileID, asList(AS_NS + "Create"), asList(INDEXABLE)));

        MockEndpoint.assertIsSatisfied(deleteEndpoint, updateEndpoint);

    }

    @DirtiesContext
    @Test
    public void testUpdateRouter() throws Exception {

        final String document = IOUtils.toString(loadResourceAsStream("container.nt"), "UTF-8").trim();
        final String responsePrefix =
                "DELETE WHERE { <" + baseURL + fileID + "> ?p ?o };\n" +
                        "INSERT DATA { ";

        final var context = camelContext.adapt(ModelCamelContext.class);

        AdviceWith.adviceWith(context, "FcrepoTriplestoreUpdater", a -> {
            a.mockEndpointsAndSkip("fcrepo*");
            a.mockEndpointsAndSkip("http*");
        });

        final MockEndpoint endpoint = MockEndpoint.resolve(camelContext, "mock:http:localhost:8080/fuseki/test/update");

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

        endpoint.assertIsSatisfied();
    }

    private static Map<String, Object> createEvent(final String identifier, final List<String> eventTypes) {
        return createEvent(identifier, eventTypes, emptyList());
    }

    @DirtiesContext
    @Test
    public void testDeleteRouter() throws Exception {

        final String eventTypes = REPOSITORY + "NODE_REMOVED";
        final String eventProps = REPOSITORY + "hasContent";

        final var context = camelContext.adapt(ModelCamelContext.class);

        AdviceWith.adviceWith(context, "FcrepoTriplestoreDeleter", a -> {
            a.mockEndpointsAndSkip("http*");
        });

        final MockEndpoint endpoint = MockEndpoint.resolve(camelContext, "mock:http:localhost:8080/fuseki/test/update");
        endpoint.expectedMessageCount(1);
        endpoint.expectedHeaderReceived(Exchange.CONTENT_TYPE, "application/x-www-form-urlencoded; charset=utf-8");
        endpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");

        template.sendBodyAndHeaders("direct:delete.triplestore", "",
                createEvent(fileID, asList(AS_NS + "Delete")));

        endpoint.assertIsSatisfied();
    }

    @Configuration
    @ComponentScan(basePackages = "org.fcrepo.camel")
    static class ContextConfig extends CamelConfiguration {
        @Bean
        public ActiveMQComponent broker() {
            final var component = new ActiveMQComponent();
            component.setBrokerURL("tcp://localhost:61616");
            return component;
        }
    }
}
