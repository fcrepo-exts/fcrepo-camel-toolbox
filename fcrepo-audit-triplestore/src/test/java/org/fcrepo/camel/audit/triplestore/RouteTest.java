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
package org.fcrepo.camel.audit.triplestore;

import static org.fcrepo.camel.RdfNamespaces.REPOSITORY;
import static org.fcrepo.camel.audit.triplestore.AuditSparqlProcessor.AUDIT;
import static org.fcrepo.camel.audit.triplestore.AuditSparqlProcessor.PREMIS;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.fcrepo.camel.JmsHeaders;

import org.junit.Test;

/**
 * Test the route workflow.
 *
 * @author escowles
 * @author Aaron Coburn
 * @since 2015-04-10
 */
public class RouteTest extends CamelBlueprintTestSupport {

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    private static final String baseURL = "http://localhost/rest";
    private static final String nodeID = "/foo";
    private static final String fileID = "/file1";
    private static final String auditContainer = "/audit";
    private static final String auditNode = auditContainer + "/1234";
    private static final String similarNode = auditContainer + "ions/1234";
    private static final long timestamp = 1428360320168L;
    private static final String eventDate = "2015-04-06T22:45:20Z";
    private static final String userID = "bypassAdmin";
    private static final String userAgent = "curl/7.37.1";

    @Override
    protected String getBlueprintDescriptor() {
        return "/OSGI-INF/blueprint/blueprint-test.xml";
    }

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
         final Properties props = new Properties();
         props.put("audit.container", auditContainer);
         props.put("input.stream", "seda:foo");
         return props;
    }

    @Test
    public void testWithoutJms() throws Exception {

        context.getRouteDefinition("AuditFcrepoRouter").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
            }
        });

        context.getRouteDefinition("AuditEventRouter").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                mockEndpointsAndSkip("http4*");
                weaveAddLast().to("mock:result");
            }
        });

        resultEndpoint.expectedMessageCount(2);
        resultEndpoint.expectedHeaderReceived(Exchange.CONTENT_TYPE, "application/x-www-form-urlencoded");
        resultEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");
        resultEndpoint.expectedHeaderReceived(AuditHeaders.EVENT_BASE_URI, "http://example.com/event");

        final String eventTypes = REPOSITORY + "NODE_REMOVED";
        final String eventProps = REPOSITORY + "hasContent";
        template.sendBodyAndHeaders("", createEvent(fileID, eventTypes, eventProps));
        template.sendBodyAndHeaders("", createEvent(similarNode, eventTypes, eventProps));
        template.sendBodyAndHeaders("", createEvent(auditNode, REPOSITORY + "NODE_ADDED", REPOSITORY + "lastModified"));
        template.sendBodyAndHeaders("", createEvent(auditContainer, REPOSITORY + "PROPERTY_CHANGED",
                REPOSITORY + "lastModified"));

        assertMockEndpointsSatisfied();
        final String body = (String)resultEndpoint.assertExchangeReceived(0).getIn().getBody();
        assertTrue("Event type not found!",
            body.contains("<" + PREMIS + "hasEventType> <" + AUDIT + "contentRemoval>"));
        assertTrue("Object link not found!",
            body.contains("<" + PREMIS + "hasEventRelatedObject> <" + baseURL + fileID + ">"));
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
