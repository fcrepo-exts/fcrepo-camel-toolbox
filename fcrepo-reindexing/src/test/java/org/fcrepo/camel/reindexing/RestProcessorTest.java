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
package org.fcrepo.camel.reindexing;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.fcrepo.camel.FcrepoHeaders;

import org.junit.Test;

/**
 * Test the route workflow.
 *
 * @author Aaron Coburn
 * @since 2015-04-10
 */
public class RestProcessorTest extends CamelTestSupport {

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    @Test
    public void testRestProcessor() throws Exception {

        resultEndpoint.expectedMessageCount(3);
        resultEndpoint.expectedHeaderValuesReceivedInAnyOrder(FcrepoHeaders.FCREPO_IDENTIFIER,
                "/", "/foo/bar", "/foo/bar");
        resultEndpoint.message(0).header(ReindexingHeaders.RECIPIENTS).isEqualTo("");
        resultEndpoint.message(1).header(ReindexingHeaders.RECIPIENTS).contains("activemq:queue:bar");
        resultEndpoint.message(1).header(ReindexingHeaders.RECIPIENTS).contains("activemq:queue:foo");
        resultEndpoint.message(2).header(ReindexingHeaders.RECIPIENTS).isEqualTo("");

        final Map<String, Object> headers = new HashMap<>();
        headers.put(Exchange.HTTP_PATH, "/");
        template.sendBodyAndHeaders("", headers);

        headers.clear();
        headers.put(Exchange.HTTP_PATH, "/foo/bar");
        headers.put(ReindexingHeaders.RECIPIENTS,
                "activemq:queue:foo, activemq:queue:bar,\t\nactivemq:queue:foo   ");
        template.sendBodyAndHeaders("", headers);

        headers.clear();
        headers.put(Exchange.HTTP_PATH, "/foo/bar");
        headers.put(ReindexingHeaders.RECIPIENTS, null);
        template.sendBodyAndHeaders("", headers);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testRestProcessorWithBody1() throws Exception {
        final String body = "[\"activemq:queue:foo\",\"activemq:queue:bar\"]";

        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.message(0).header(ReindexingHeaders.RECIPIENTS).contains("activemq:queue:bar");
        resultEndpoint.message(0).header(ReindexingHeaders.RECIPIENTS).contains("activemq:queue:foo");

        final Map<String, Object> headers = new HashMap<>();
        headers.put(Exchange.CONTENT_TYPE, "application/json");
        headers.put(Exchange.HTTP_PATH, "/foo/bar");
        template.sendBodyAndHeaders(body, headers);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testRestProcessorWithBody2() throws Exception {
        final String body = "[\"activemq:queue:foo\",\"activemq:queue:bar\"]";

        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.message(0).header(ReindexingHeaders.RECIPIENTS).contains("activemq:queue:bar");
        resultEndpoint.message(0).header(ReindexingHeaders.RECIPIENTS).contains("activemq:queue:foo");
        resultEndpoint.message(0).header(ReindexingHeaders.RECIPIENTS).contains("activemq:queue:baz");

        final Map<String, Object> headers = new HashMap<>();
        headers.put(Exchange.HTTP_PATH, "/foo/bar");
        headers.put(Exchange.CONTENT_TYPE, "application/json");
        headers.put(ReindexingHeaders.RECIPIENTS, "activemq:queue:baz");
        template.sendBodyAndHeaders(body, headers);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testRestProcessorWithNullBody() throws Exception {

        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.message(0).header(ReindexingHeaders.RECIPIENTS).isEqualTo("activemq:queue:baz");

        final Map<String, Object> headers = new HashMap<>();
        headers.put(Exchange.CONTENT_TYPE, "application/json");
        headers.put(Exchange.HTTP_PATH, "/foo/bar");
        headers.put(ReindexingHeaders.RECIPIENTS, "activemq:queue:baz");
        template.sendBodyAndHeaders(null, headers);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testRestProcessorWithNoContentType() throws Exception {

        final String body = "[\"activemq:queue:foo\",\"activemq:queue:bar\"]";

        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.message(0).header(ReindexingHeaders.RECIPIENTS).isEqualTo("activemq:queue:baz");

        final Map<String, Object> headers = new HashMap<>();
        headers.put(Exchange.HTTP_PATH, "/foo/bar");
        headers.put(ReindexingHeaders.RECIPIENTS, "activemq:queue:baz");
        template.sendBodyAndHeaders(body, headers);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testRestProcessorWithBadContentType() throws Exception {

        final String body = "[\"activemq:queue:foo\",\"activemq:queue:bar\"]";

        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.message(0).header(ReindexingHeaders.RECIPIENTS).isEqualTo("activemq:queue:baz");

        final Map<String, Object> headers = new HashMap<>();
        headers.put(Exchange.CONTENT_TYPE, "application/foo");
        headers.put(Exchange.HTTP_PATH, "/foo/bar");
        headers.put(ReindexingHeaders.RECIPIENTS, "activemq:queue:baz");
        template.sendBodyAndHeaders(body, headers);

        assertMockEndpointsSatisfied();
    }


    @Test
    public void testRestProcessorWithEmptyBody() throws Exception {

        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.message(0).header(ReindexingHeaders.RECIPIENTS).isEqualTo("activemq:queue:baz");

        final Map<String, Object> headers = new HashMap<>();
        headers.put(Exchange.CONTENT_TYPE, "application/json");
        headers.put(Exchange.HTTP_PATH, "/foo/bar");
        headers.put(ReindexingHeaders.RECIPIENTS, "activemq:queue:baz");
        template.sendBodyAndHeaders("    ", headers);

        assertMockEndpointsSatisfied();
    }


    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() throws IOException {
                from("direct:start")
                    .process(new RestProcessor())
                    .to("mock:result");
            }
        };
    }
}
