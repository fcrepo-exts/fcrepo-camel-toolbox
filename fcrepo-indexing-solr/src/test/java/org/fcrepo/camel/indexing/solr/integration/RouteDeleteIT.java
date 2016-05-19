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
package org.fcrepo.camel.indexing.solr.integration;

import static com.jayway.awaitility.Awaitility.await;
import static org.fcrepo.camel.RdfNamespaces.REPOSITORY;
import static org.hamcrest.Matchers.equalTo;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.camel.util.ObjectHelper;
import org.fcrepo.camel.JmsHeaders;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.camel.FcrepoHeaders;
import org.fcrepo.client.FcrepoResponse;
import org.fcrepo.camel.indexing.solr.SolrRouter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Test the route workflow.
 *
 * @author Aaron Coburn
 * @since 2015-04-10
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/spring-test/test-container.xml"})
public class RouteDeleteIT extends CamelTestSupport {

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    private String fullPath = "";

    @Override
    protected void doPreSetup() throws Exception {
        final String webPort = System.getProperty("fcrepo.dynamic.test.port", "8080");
        final String jettyPort = System.getProperty("jetty.dynamic.test.port", "8080");
        final FcrepoClient client = new FcrepoClient(null, null, null, true);
        final FcrepoResponse res = client.post(
                URI.create("http://localhost:" + webPort + "/rest"),
                ObjectHelper.loadResourceAsStream("indexable.ttl"),
                "text/turtle");
        fullPath = res.getLocation().toString();
        TestUtils.httpPost("http://localhost:" + jettyPort + "/solr/testCore/update?commit=true",
                "<delete><query>*:*</query></delete>", "application/xml");
        final String solrDoc = "[{\"id\":[\"" + fullPath + "\"]}]";
        TestUtils.httpPost("http://localhost:" + jettyPort + "/solr/testCore/update?commit=true",
                solrDoc, "application/json");
    }

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
        final String webPort = System.getProperty("fcrepo.dynamic.test.port", "8080");
        final String jettyPort = System.getProperty("jetty.dynamic.test.port", "8080");
        final String jmsPort = System.getProperty("fcrepo.dynamic.jms.port", "61616");

        final Properties props = new Properties();
        props.put("indexing.predicate", "true");
        props.put("error.maxRedeliveries", "10");
        props.put("indexing.predicate", "true");
        props.put("fcrepo.baseUrl", "localhost:" + webPort + "/rest");
        props.put("fcrepo.defaultTransform", "default");
        props.put("solr.baseUrl", "localhost:" + jettyPort + "/solr/testCore");
        props.put("solr.commitWithin", "100");
        props.put("input.stream", "direct:start");
        props.put("solr.reindex.stream", "seda:reindex");
        props.put("audit.container", "/audit");
        return props;
    }

    @Test
    public void testDeletedJmsEventRouter() throws Exception {
        final String jettyPort = System.getProperty("jetty.dynamic.test.port", "8080");
        final String webPort = System.getProperty("fcrepo.dynamic.test.port", "8080");
        final String jmsPort = System.getProperty("fcrepo.dynamic.jms.port", "61616");
        final String path = fullPath.replaceFirst("http://localhost:[0-9]+/rest", "");
        final String solrEndpoint = "mock:http4:localhost:" + jettyPort + "/solr/testCore/update";
        final String fcrepoEndpoint = "mock:fcrepo:localhost:" + webPort + "/rest";
        final String url = "http://localhost:" + jettyPort + "/solr/testCore/select?q=*&wt=json";

        context.getRouteDefinition("FcrepoSolrRouter").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                mockEndpoints("*");
            }
        });
        context.getRouteDefinition("FcrepoSolrDeleter").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                mockEndpoints("*");
            }
        });
        context.start();

        final Map<String, Object> headers = new HashMap<>();
        headers.put(JmsHeaders.IDENTIFIER, path);
        headers.put(JmsHeaders.BASE_URL, "http://localhost:" + webPort + "/rest");
        headers.put(JmsHeaders.EVENT_TYPE, REPOSITORY + "NODE_REMOVED");
        headers.put(JmsHeaders.TIMESTAMP, 1428360320168L);
        headers.put(JmsHeaders.PROPERTIES, "");

        getMockEndpoint(solrEndpoint).expectedMessageCount(1);
        getMockEndpoint("mock://direct:delete.solr").expectedMessageCount(1);
        getMockEndpoint("mock://direct:update.solr").expectedMessageCount(0);
        getMockEndpoint(fcrepoEndpoint).expectedMessageCount(0);

        template.sendBodyAndHeaders("direct:start", "", headers);

        assertMockEndpointsSatisfied();

        await().until(TestUtils.solrCount(url), equalTo(0));
    }

    @Test
    public void testDeletedInternalEventRouter() throws Exception {
        final String webPort = System.getProperty("fcrepo.dynamic.test.port", "8080");
        final String jettyPort = System.getProperty("jetty.dynamic.test.port", "8080");
        final String jmsPort = System.getProperty("fcrepo.dynamic.jms.port", "61616");
        final String path = fullPath.replaceFirst("http://localhost:[0-9]+/rest", "");
        final String solrEndpoint = "mock:http4:localhost:" + jettyPort + "/solr/testCore/update";
        final String fcrepoEndpoint = "mock:fcrepo:localhost:" + webPort + "/rest";
        final String url = "http://localhost:" + jettyPort + "/solr/testCore/select?q=*&wt=json";

        context.getRouteDefinition("FcrepoSolrRouter").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                mockEndpoints("*");
            }
        });
        context.getRouteDefinition("FcrepoSolrDeleter").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                mockEndpoints("*");
            }
        });
        context.start();

        final Map<String, Object> headers = new HashMap<>();
        headers.put(FcrepoHeaders.FCREPO_IDENTIFIER, path);
        headers.put(FcrepoHeaders.FCREPO_BASE_URL, "http://localhost:" + webPort + "/rest");
        headers.put(JmsHeaders.EVENT_TYPE, REPOSITORY + "NODE_REMOVED");

        getMockEndpoint(solrEndpoint).expectedMessageCount(1);
        getMockEndpoint("mock://direct:delete.solr").expectedMessageCount(1);
        getMockEndpoint("mock://direct:update.solr").expectedMessageCount(0);
        getMockEndpoint(fcrepoEndpoint).expectedMessageCount(0);

        template.sendBodyAndHeaders("direct:start", "", headers);

        assertMockEndpointsSatisfied();

        await().until(TestUtils.solrCount(url), equalTo(0));
    }
}
