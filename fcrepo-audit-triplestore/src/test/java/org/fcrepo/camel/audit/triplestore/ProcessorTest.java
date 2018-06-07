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
package org.fcrepo.camel.audit.triplestore;

import static java.util.Arrays.asList;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_AGENT;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_DATE_TIME;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_EVENT_TYPE;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_EVENT_ID;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_RESOURCE_TYPE;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.fcrepo.camel.audit.triplestore.AuditSparqlProcessor.AUDIT;
import static org.fcrepo.camel.audit.triplestore.AuditSparqlProcessor.EVENT_TYPE;
import static org.fcrepo.camel.audit.triplestore.AuditSparqlProcessor.PREMIS;
import static org.fcrepo.camel.audit.triplestore.AuditSparqlProcessor.XSD;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
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
    private static final String eventDate = "2015-04-06T22:45:20Z";
    private static final String userID = "bypassAdmin";
    private static final String userAgent = "curl/7.37.1";
    private static final String eventID = "ab/cd/ef/gh/abcdefgh12345678";
    private static final String eventURI = eventBaseURI + "/" + eventID;
    private static final String EVENT_NS = "http://fedora.info/definitions/v4/event#";
    private static final String AS_NS = "https://www.w3.org/ns/activitystreams#";
    private static final String REPOSITORY = "http://fedora.info/definitions/v4/repository#";

    @Test
    public void testNodeAdded() throws Exception {

        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.expectedHeaderReceived(Exchange.CONTENT_TYPE, "application/x-www-form-urlencoded");
        resultEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");

        template.sendBodyAndHeaders("",
                createEvent(nodeID, asList(EVENT_NS + "ResourceCreation"), asList(REPOSITORY + "Resource"), eventID));

        assertMockEndpointsSatisfied();
        final String body = (String)resultEndpoint.assertExchangeReceived(0).getIn().getBody();
        assertTrue("Event type not found!",
            body.contains("<" + eventURI + "> <" + PREMIS + "hasEventType> <" + EVENT_TYPE + "cre>"));
        assertTrue("Object link not found!",
            body.contains("<" + eventURI + "> <" + PREMIS + "hasEventRelatedObject> <" + baseURL + nodeID + ">"));
        assertTrue("Event date not found!",
            body.contains("<" + eventURI + "> <" + PREMIS + "hasEventDateTime> \"" + eventDate + "\"^^<"
                    + XSD + "dateTime>"));
        assertTrue("Event user not found!",
            body.contains("<" + eventURI + "> <" + PREMIS + "hasEventRelatedAgent> \"" + userID + "\" ."));
        assertTrue("Event agent not found!",
            body.contains("<" + eventURI + "> <" + PREMIS + "hasEventRelatedAgent> \"" + userAgent + "\" ."));
    }

    @Test
    public void testNodeRemoved() throws Exception {

        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.expectedHeaderReceived(Exchange.CONTENT_TYPE, "application/x-www-form-urlencoded");
        resultEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");

        template.sendBodyAndHeaders("",
                createEvent(nodeID, asList(EVENT_NS + "ResourceDeletion"), asList(REPOSITORY + "Resource"), eventID));

        assertMockEndpointsSatisfied();
        final String body = (String)resultEndpoint.assertExchangeReceived(0).getIn().getBody();
        assertTrue("Event type not found!",
            body.contains("<" + eventURI + "> <" + PREMIS + "hasEventType> <" + EVENT_TYPE + "del>"));
        assertTrue("Object link not found!",
            body.contains("<" + eventURI + "> <" + PREMIS + "hasEventRelatedObject> <" + baseURL + nodeID + ">"));
    }

    @Test
    public void testPropertiesChanged() throws Exception {

        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.expectedHeaderReceived(Exchange.CONTENT_TYPE, "application/x-www-form-urlencoded");
        resultEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");

        template.sendBodyAndHeaders("",
                createEvent(nodeID, asList(AS_NS + "Update"), asList(REPOSITORY + "Resource"), eventID));

        assertMockEndpointsSatisfied();
        final String body = (String)resultEndpoint.assertExchangeReceived(0).getIn().getBody();
        assertTrue("Event type not found!",
            body.contains("<" + eventURI + "> <" + PREMIS + "hasEventType> <" + AUDIT + "metadataModification>"));
        assertTrue("Object link not found!",
            body.contains("<" + eventURI + "> <" + PREMIS + "hasEventRelatedObject> <" + baseURL + nodeID + ">"));
    }

    @Test
    public void testFileAdded() throws Exception {

        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.expectedHeaderReceived(Exchange.CONTENT_TYPE, "application/x-www-form-urlencoded");
        resultEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");

        template.sendBodyAndHeaders("",
                createEvent(fileID, asList(AS_NS + "Create"), asList(REPOSITORY + "Binary"), eventID));

        assertMockEndpointsSatisfied();
        final String body = (String)resultEndpoint.assertExchangeReceived(0).getIn().getBody();
        assertTrue("Event type not found!",
            body.contains("<" + eventURI + "> <" + PREMIS + "hasEventType> <" + EVENT_TYPE + "ing>"));
        assertTrue("Object link not found!",
            body.contains("<" + eventURI + "> <" + PREMIS + "hasEventRelatedObject> <" + baseURL + fileID + ">"));
    }

    @Test
    public void testFileChanged() throws Exception {

        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.expectedHeaderReceived(Exchange.CONTENT_TYPE, "application/x-www-form-urlencoded");
        resultEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");

        template.sendBodyAndHeaders("",
                createEvent(fileID, asList(AS_NS + "Update"), asList(REPOSITORY + "Binary"), eventID));

        assertMockEndpointsSatisfied();
        final String body = (String)resultEndpoint.assertExchangeReceived(0).getIn().getBody();
        assertTrue("Event type not found!",
            body.contains("<" + eventURI + "> <" + PREMIS + "hasEventType> <" + AUDIT + "contentModification>"));
        assertTrue("Object link not found!",
            body.contains("<" + eventURI + "> <" + PREMIS + "hasEventRelatedObject> <" + baseURL + fileID + ">"));
    }

    @Test
    public void testFileRemoved() throws Exception {

        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.expectedHeaderReceived(Exchange.CONTENT_TYPE, "application/x-www-form-urlencoded");
        resultEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");

        template.sendBodyAndHeaders("",
                createEvent(fileID, asList(AS_NS + "Delete"), asList(REPOSITORY + "Binary"), eventID));

        assertMockEndpointsSatisfied();
        final String body = (String)resultEndpoint.assertExchangeReceived(0).getIn().getBody();
        assertTrue("Event type not found!",
            body.contains("<" + eventURI + "> <" + PREMIS + "hasEventType> <" + AUDIT + "contentRemoval>"));
        assertTrue("Object link not found!",
            body.contains("<" + eventURI + "> <" + PREMIS + "hasEventRelatedObject> <" + baseURL + fileID + ">"));
    }

    private static Map<String,Object> createEvent(final String identifier, final List<String> eventTypes,
            final List<String> resourceTypes, final String eventID) {

        final Map<String, Object> headers = new HashMap<>();
        headers.put(FCREPO_URI, baseURL + identifier);
        headers.put(FCREPO_DATE_TIME, eventDate);
        headers.put(FCREPO_AGENT, asList(userID, userAgent));
        headers.put(FCREPO_RESOURCE_TYPE, resourceTypes);
        headers.put(FCREPO_EVENT_TYPE, eventTypes);
        headers.put(FCREPO_EVENT_ID, eventID);
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
