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
package org.fcrepo.camel.indexing.triplestore;

import static org.fcrepo.camel.RdfNamespaces.REPOSITORY;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.camel.util.ObjectHelper;
import org.apache.commons.io.IOUtils;
import org.fcrepo.camel.JmsHeaders;

import org.junit.Test;

/**
 * Test the route workflow.
 *
 * @author Aaron Coburn
 * @since 2015-04-22
 */
public class RouteTest extends CamelBlueprintTestSupport {

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
        return "/OSGI-INF/blueprint/blueprint.xml";
    }

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
         final Properties props = new Properties();
         props.put("indexing.predicate", "true");
         return props;
    }

    @Test
    public void testEventTypeRouter() throws Exception {

        final String eventTypes = REPOSITORY + "NODE_REMOVED";
        final String eventProps = REPOSITORY + "hasContent";

        context.getRouteDefinition("FcrepoTriplestoreRouter").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpointsAndSkip("*");
            }
        });
        context.start();

        getMockEndpoint("mock:direct:delete.triplestore").expectedMessageCount(1);
        getMockEndpoint("mock:direct.update.triplestore").expectedMessageCount(0);

        template.sendBodyAndHeaders(
                IOUtils.toString(ObjectHelper.loadResourceAsStream("indexable.rdf"), "UTF-8"),
                createEvent(fileID, eventTypes, eventProps));

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testPrepareRouterIndexable() throws Exception {

        final String eventTypes = REPOSITORY + "NODE_ADDED";
        final String eventProps = REPOSITORY + "hasContent";

        context.getRouteDefinition("FcrepoTriplestoreRouter").adviceWith(context, new AdviceWithRouteBuilder() {
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
                IOUtils.toString(ObjectHelper.loadResourceAsStream("indexable.rdf"), "UTF-8"),
                createEvent(fileID, eventTypes, eventProps));

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testPrepareRouterContainer() throws Exception {

        final String eventTypes = REPOSITORY + "NODE_ADDED";
        final String eventProps = REPOSITORY + "hasContent";
        final PropertiesComponent pc = context.getComponent("properties", PropertiesComponent.class);
        pc.setCache(false);
        System.setProperty("indexing.predicate", "false");

        context.getRouteDefinition("FcrepoTriplestoreRouter").adviceWith(context, new AdviceWithRouteBuilder() {
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
                IOUtils.toString(ObjectHelper.loadResourceAsStream("container.rdf"), "UTF-8"),
                createEvent(fileID, eventTypes, eventProps));

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testUpdateRouter() throws Exception {

        final String document = IOUtils.toString(ObjectHelper.loadResourceAsStream("container.nt"), "UTF-8").trim();
        final String eventTypes = REPOSITORY + "NODE_ADDED";
        final String eventProps = REPOSITORY + "hasContent";
        final String responsePrefix =
                  "update=DELETE WHERE { <" + baseURL + fileID + "> ?p ?o }; " +
                  "DELETE WHERE { <" + baseURL + fileID + "/fcr:export?format=jcr/xml> ?p ?o }; " +
                  "INSERT DATA { ";
        final String responseSuffix = " }";


        context.getRouteDefinition("FcrepoTriplestoreUpdater").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                mockEndpointsAndSkip("fcrepo*");
                mockEndpointsAndSkip("http4*");
            }
        });

        context.start();

        final MockEndpoint endpoint = getMockEndpoint("mock:http4:localhost:3030/test/update");

        endpoint.expectedMessageCount(1);
        endpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");
        endpoint.expectedHeaderReceived(Exchange.CONTENT_TYPE, "application/x-www-form-urlencoded");
        endpoint.allMessages().body().regexReplaceAll("\\s+", " ").startsWith(responsePrefix);
        endpoint.allMessages().body().regexReplaceAll("\\s+", " ").endsWith(responseSuffix);
        for (final String s : document.split("\n")) {
            endpoint.expectedBodyReceived().body().contains(s);
        }

        final Map<String, Object> headers = createEvent(fileID, eventTypes, eventProps);
        headers.put(Exchange.CONTENT_TYPE, "application/rdf+xml");

        template.sendBodyAndHeaders("direct:update.triplestore",
                IOUtils.toString(ObjectHelper.loadResourceAsStream("container.rdf"), "UTF-8"),
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
                mockEndpointsAndSkip("http4*");
            }
        });

        context.start();

        getMockEndpoint("mock:http4:localhost:3030/test/update").expectedMessageCount(1);
        getMockEndpoint("mock:http4:localhost:3030/test/update")
            .expectedHeaderReceived(Exchange.CONTENT_TYPE, "application/x-www-form-urlencoded");
        getMockEndpoint("mock:http4:localhost:3030/test/update")
            .expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");

        template.sendBodyAndHeaders("direct:delete.triplestore", "",
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
