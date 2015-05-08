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
package org.fcrepo.camel.reindexing;

import static org.fcrepo.camel.FcrepoHeaders.FCREPO_IDENTIFIER;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_BASE_URL;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.TransformDefinition;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.camel.util.ObjectHelper;

import org.junit.Test;

/**
 * Test the route workflow.
 *
 * @author acoburn
 * @since 2015-05-22
 */
public class RouteTest extends CamelBlueprintTestSupport {

    private static final String restPrefix = "/reindexing";
    private static final String reindexingStream = "activemq:queue:foo";

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

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

        final String restPort = System.getProperty("reindexing.dynamic.test.port", "9080");

        final Properties props = new Properties();
        props.put("reindexing.stream", reindexingStream);
        props.put("rest.prefix", restPrefix);
        props.put("rest.port", restPort);
        return props;
    }

    @Test
    public void testUsageRoute() throws Exception {

        final String restPort = System.getProperty("reindexing.dynamic.test.port", "9080");

        context.getRouteDefinition("FcrepoReindexingUsage").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveAddLast().to("mock:result");
            }
        });

        context.start();

        getMockEndpoint("mock:result").expectedMessageCount(1);
        getMockEndpoint("mock:result").message(0).body().contains("Fedora Reindexing Service");
        getMockEndpoint("mock:result").message(0).body().contains(
                InetAddress.getLocalHost().getHostName() + ":" + restPort + restPrefix);

        template.sendBody("direct:usage", null);

        assertMockEndpointsSatisfied();
    }


    @Test
    public void testReindexNoEndpointsRoute() throws Exception {
        final String id = "/foo";

        context.getRouteDefinition("FcrepoReindexingReindex").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                mockEndpointsAndSkip(reindexingStream + "?disableTimeToLive=true");
                weaveByType(TransformDefinition.class).after().to("mock:result");
            }
        });
        context.start();

        getMockEndpoint("mock:" + reindexingStream).expectedMessageCount(0);
        getMockEndpoint("mock:result").expectedMessageCount(1);
        getMockEndpoint("mock:result").expectedHeaderReceived(FCREPO_IDENTIFIER, id);
        getMockEndpoint("mock:result").expectedBodiesReceived("No endpoints configured for indexing");


        final Map<String, Object> headers = new HashMap<>();
        headers.put(Exchange.HTTP_URI, "http://localhost" + restPrefix + id);
        headers.put(ReindexingHeaders.RECIPIENTS, "");

        template.sendBodyAndHeaders("direct:reindex", null, headers);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testReindexWithEndpointsRoute() throws Exception {
        final String id = "/foo";

        context.getRouteDefinition("FcrepoReindexingReindex").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                mockEndpointsAndSkip(reindexingStream + "?disableTimeToLive=true");
                weaveByType(TransformDefinition.class).after().to("mock:result");
            }
        });
        context.start();

        getMockEndpoint("mock:" + reindexingStream).expectedMessageCount(1);
        getMockEndpoint("mock:result").expectedMessageCount(1);
        getMockEndpoint("mock:result").expectedHeaderReceived(FCREPO_IDENTIFIER, id);
        getMockEndpoint("mock:result").expectedBodiesReceived("Indexing started at " + id);

        final Map<String, Object> headers = new HashMap<>();
        headers.put(Exchange.HTTP_URI, "http://localhost" + restPrefix + id);
        headers.put(ReindexingHeaders.RECIPIENTS, "mock:endpoint");

        template.sendBodyAndHeaders("direct:reindex", null, headers);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testTraversal() throws Exception {

        getMockEndpoint("mock:direct:recipients").expectedMessageCount(1);
        getMockEndpoint("mock:" + reindexingStream).expectedMessageCount(7);
        getMockEndpoint("mock:" + reindexingStream).expectedHeaderValuesReceivedInAnyOrder(FCREPO_IDENTIFIER,
                "/foo/a", "/foo/b", "/foo/c", "/foo/d", "/foo/e", "/foo/f", "/foo/g");

        context.getRouteDefinition("FcrepoReindexingTraverse").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:traverse");
                mockEndpointsAndSkip("fcrepo:*");
                mockEndpointsAndSkip(reindexingStream + "*");
                mockEndpointsAndSkip("direct:recipients");
            }
        });
        context.start();

        template.sendBodyAndHeader("direct:traverse", ObjectHelper.loadResourceAsStream("indexable.rdf"),
                FCREPO_BASE_URL, "http://localhost:8080/fcrepo4/rest");

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testRecipientList() throws Exception {
        final String id = "/foo";

        getMockEndpoint("mock:foo").expectedMessageCount(1);
        getMockEndpoint("mock:foo").expectedHeaderReceived(FCREPO_IDENTIFIER, id);
        getMockEndpoint("mock:bar").expectedMessageCount(1);
        getMockEndpoint("mock:bar").expectedHeaderReceived(FCREPO_IDENTIFIER, id);

        final Map<String, Object> headers = new HashMap<>();
        headers.put(FCREPO_IDENTIFIER, id);
        headers.put(ReindexingHeaders.RECIPIENTS, "mock:foo,mock:bar");

        template.sendBodyAndHeaders("direct:recipients", null, headers);

        assertMockEndpointsSatisfied();
    }
}
