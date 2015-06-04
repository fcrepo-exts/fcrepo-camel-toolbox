/**
 * Copyright 2015 DuraSpace, Inc.
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
import static org.fcrepo.audit.AuditNamespaces.AUDIT;
import static org.fcrepo.audit.AuditNamespaces.EVENT_TYPE;
import static org.fcrepo.audit.AuditNamespaces.PREMIS;
import static org.fcrepo.audit.AuditNamespaces.XSD;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.fcrepo.camel.JmsHeaders;
import org.apache.camel.test.junit4.CamelTestSupport;

import org.junit.Test;

/**
 * Test the route workflow.
 *
 * @author escowles
 * @author Aaron Coburn
 * @since 2015-04-10
 */
public class ProcessorTest extends CamelTestSupport {

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    private static final String baseURL = "http://localhost/rest";
    private static final String eventBaseURI = "http://example.com/event";
    private static final String nodeID = "/foo";
    private static final String fileID = "/file1";
    private static final long timestamp = 1428360320168L;
    private static final String eventDate = "2015-04-06T22:45:20Z";
    private static final String userID = "bypassAdmin";
    private static final String userAgent = "curl/7.37.1";

    @Test
    public void testNodeAdded() throws Exception {

        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.expectedHeaderReceived(Exchange.CONTENT_TYPE, "application/x-www-form-urlencoded");
        resultEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");

        final String eventTypes = REPOSITORY + "NODE_ADDED," + REPOSITORY + "PROPERTY_ADDED";
        final String eventProps = REPOSITORY + "lastModified," + REPOSITORY + "primaryType," +
                REPOSITORY + "lastModifiedBy," + REPOSITORY + "created," + REPOSITORY + "mixinTypes," +
                REPOSITORY + "createdBy";
        template.sendBodyAndHeaders("", createEvent(nodeID, eventTypes, eventProps));

        assertMockEndpointsSatisfied();
        final String body = (String)resultEndpoint.assertExchangeReceived(0).getIn().getBody();
        assertTrue("Event type not found!",
            body.contains("<" + PREMIS + "hasEventType> <" + EVENT_TYPE + "cre>"));
        assertTrue("Object link not found!",
            body.contains("<" + PREMIS + "hasEventRelatedObject> <" + baseURL + nodeID + ">"));
        assertTrue("Event date not found!",
            body.contains("<" + PREMIS + "hasEventDateTime> \"" + eventDate + "\"^^<" + XSD + "dateTime>"));
        assertTrue("Event user not found!",
            body.contains("<" + PREMIS + "hasEventRelatedAgent> \"" + userID + "\"^^<" + XSD + "string>"));
        assertTrue("Event agent not found!",
            body.contains("<" + PREMIS + "hasEventRelatedAgent> \"" + userAgent + "\"^^<" + XSD + "string>"));
    }

    @Test
    public void testNodeRemoved() throws Exception {

        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.expectedHeaderReceived(Exchange.CONTENT_TYPE, "application/x-www-form-urlencoded");
        resultEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");

        final String eventTypes = REPOSITORY + "NODE_REMOVED";
        template.sendBodyAndHeaders("", createEvent(nodeID, eventTypes, null));

        assertMockEndpointsSatisfied();
        final String body = (String)resultEndpoint.assertExchangeReceived(0).getIn().getBody();
        assertTrue("Event type not found!",
            body.contains("<" + PREMIS + "hasEventType> <" + EVENT_TYPE + "del>"));
        assertTrue("Object link not found!",
            body.contains("<" + PREMIS + "hasEventRelatedObject> <" + baseURL + nodeID + ">"));
    }

    @Test
    public void testPropertiesChanged() throws Exception {

        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.expectedHeaderReceived(Exchange.CONTENT_TYPE, "application/x-www-form-urlencoded");
        resultEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");

        final String eventTypes = REPOSITORY + "PROPERTY_CHANGED," + REPOSITORY + "PROPERTY_ADDED";
        final String eventProps = REPOSITORY + "lastModified,http://purl.org/dc/elements/1.1/title";
        template.sendBodyAndHeaders("", createEvent(nodeID, eventTypes, eventProps));

        assertMockEndpointsSatisfied();
        final String body = (String)resultEndpoint.assertExchangeReceived(0).getIn().getBody();
        assertTrue("Event type not found!",
            body.contains("<" + PREMIS + "hasEventType> <" + AUDIT + "metadataModification>"));
        assertTrue("Object link not found!",
            body.contains("<" + PREMIS + "hasEventRelatedObject> <" + baseURL + nodeID + ">"));
    }

    @Test
    public void testFileAdded() throws Exception {

        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.expectedHeaderReceived(Exchange.CONTENT_TYPE, "application/x-www-form-urlencoded");
        resultEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");

        final String eventTypes = REPOSITORY + "NODE_ADDED," + REPOSITORY + "PROPERTY_ADDED";
        final String eventProps = REPOSITORY + "lastModified," + REPOSITORY + "primaryType," +
                REPOSITORY + "lastModifiedBy," + REPOSITORY + "created," + REPOSITORY + "mixinTypes," +
                REPOSITORY + "createdBy," + REPOSITORY + "hasContent," +
                PREMIS + "hasSize," + PREMIS + "hasOriginalName," + REPOSITORY + "digest";
        template.sendBodyAndHeaders("", createEvent(fileID, eventTypes, eventProps));

        assertMockEndpointsSatisfied();
        final String body = (String)resultEndpoint.assertExchangeReceived(0).getIn().getBody();
        assertTrue("Event type not found!",
            body.contains("<" + PREMIS + "hasEventType> <" + EVENT_TYPE + "ing>"));
        assertTrue("Object link not found!",
            body.contains("<" + PREMIS + "hasEventRelatedObject> <" + baseURL + fileID + ">"));
    }

    @Test
    public void testFileChanged() throws Exception {

        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.expectedHeaderReceived(Exchange.CONTENT_TYPE, "application/x-www-form-urlencoded");
        resultEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");

        final String eventTypes = REPOSITORY + "PROPERTY_CHANGED";
        final String eventProps = REPOSITORY + "lastModified," + REPOSITORY + "hasContent," +
                PREMIS + "hasSize," + PREMIS + "hasOriginalName," + REPOSITORY + "digest";
        template.sendBodyAndHeaders("", createEvent(fileID, eventTypes, eventProps));

        assertMockEndpointsSatisfied();
        final String body = (String)resultEndpoint.assertExchangeReceived(0).getIn().getBody();
        assertTrue("Event type not found!",
            body.contains("<" + PREMIS + "hasEventType> <" + AUDIT + "contentModification>"));
        assertTrue("Object link not found!",
            body.contains("<" + PREMIS + "hasEventRelatedObject> <" + baseURL + fileID + ">"));
    }

    @Test
    public void testFileRemoved() throws Exception {

        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.expectedHeaderReceived(Exchange.CONTENT_TYPE, "application/x-www-form-urlencoded");
        resultEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");

        final String eventTypes = REPOSITORY + "NODE_REMOVED";
        final String eventProps = REPOSITORY + "hasContent";
        template.sendBodyAndHeaders("", createEvent(fileID, eventTypes, eventProps));

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

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() throws IOException {
                from("direct:start")
                    .setHeader(AuditHeaders.EVENT_BASE_URI, constant(eventBaseURI))
                    .process(new AuditSparqlProcessor())
                    .to("mock:result");
            }
        };
    }
}
