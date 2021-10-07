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
package org.fcrepo.camel.indexing.http;

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
    private final String EVENT_NS = "https://www.w3.org/ns/activitystreams#";
    private final String INDEXABLE = "http://fedora.info/definitions/v4/indexing#Indexable";
    private static final String baseURL = "http://localhost/rest";
    private static final String httpURL = "http://localhost/http_endpoint";
    private static final String fileID = "/file1";
    private static final String eventDate = "2015-04-06T22:45:20Z";
    private static final String userID = "bypassAdmin";
    private static final String userAgent = "curl/7.37.1";
    private static final String auditContainer = "/audit";
    private static final String inputStream = "seda:foo";
    private static final String reindexStream = "seda:bar";

    @Autowired
    private CamelContext camelContext;

    @Produce("direct:start")
    protected ProducerTemplate template;

    @BeforeClass
    public static void beforeClass() {
        System.setProperty("http.indexer.enabled", "true");
        System.setProperty("http.filter.containers", baseURL + auditContainer);
        System.setProperty("http.input.stream", inputStream);
        System.setProperty("http.reindex.stream", reindexStream);
        System.setProperty("error.maxRedeliveries", "10");
        System.setProperty("fcrepo.baseUrl", baseURL);
        System.setProperty("http.baseUrl", httpURL);
        System.setProperty("http.reindex.stream", "seda:reindex");
    }

    @DirtiesContext
    @Test
    public void testEventTypeForwarding() throws Exception {
        // Let's be sure "Delete" gets passed along as expected
        final List<String> eventTypes = asList(EVENT_NS + "Delete");

        final var context = camelContext.adapt(ModelCamelContext.class);
        AdviceWith.adviceWith(context, "FcrepoHttpAddType", a -> {
            a.mockEndpointsAndSkip("direct:send.to.http");
        });

        final var sendEndpoint = MockEndpoint.resolve(camelContext, "mock:direct:send.to.http");
        sendEndpoint.expectedMessageCount(1);
        sendEndpoint.expectedHeaderReceived(FCREPO_EVENT_TYPE, "https://www.w3.org/ns/activitystreams#Delete");
        template.sendBodyAndHeaders(inputStream, "{}",
                createEvent(baseURL + fileID, eventTypes));

        MockEndpoint.assertIsSatisfied(sendEndpoint);
    }

    @DirtiesContext
    @Test
    public void testEventTypeInjection() throws Exception {
        // Let's make sure "Update" is the default when no event type is provided
        final List<String> eventTypes = emptyList();

        final var context = camelContext.adapt(ModelCamelContext.class);
        AdviceWith.adviceWith(context, "FcrepoHttpAddType", a -> {
            a.mockEndpointsAndSkip("direct:send.to.http");
        });

        final var sendEndpoint = MockEndpoint.resolve(camelContext, "mock:direct:send.to.http");
        sendEndpoint.expectedMessageCount(1);
        // We expect "Update" as default if nothing is provided
        sendEndpoint.expectedHeaderReceived(FCREPO_EVENT_TYPE, "https://www.w3.org/ns/activitystreams#Update");
        template.sendBodyAndHeaders(inputStream, "{}",
                createEvent(baseURL + fileID, eventTypes));

        MockEndpoint.assertIsSatisfied(sendEndpoint);
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
        // The HttpRouter expects to find org.fcrepo.jms.eventtype and move it into
        // FCREPO_EVENT_TYPE (or set a default)
        if (eventTypes.size() > 0) {
            headers.put("org.fcrepo.jms.eventtype", eventTypes.get(0));
        }
        headers.put(FCREPO_RESOURCE_TYPE, resourceTypes);
        return headers;
    }

    @Configuration
    @ComponentScan(resourcePattern = "**/Fcrepo*.class")
    static class ContextConfig extends CamelConfiguration {

        @Bean
        public RouteBuilder route() {
            return new HttpRouter();
        }
    } 
}
