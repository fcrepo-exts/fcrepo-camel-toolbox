/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel.reindexing;

import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.Exchange.HTTP_PATH;
import static org.fcrepo.camel.reindexing.ReindexingHeaders.REINDEXING_RECIPIENTS;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;

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
        resultEndpoint.message(0).header(REINDEXING_RECIPIENTS).isEqualTo("");
        resultEndpoint.message(1).header(REINDEXING_RECIPIENTS).contains("broker:queue:bar");
        resultEndpoint.message(1).header(REINDEXING_RECIPIENTS).contains("broker:queue:foo");
        resultEndpoint.message(2).header(REINDEXING_RECIPIENTS).isEqualTo("");

        final Map<String, Object> headers = new HashMap<>();
        headers.put(HTTP_PATH, "/");
        template.sendBodyAndHeaders("", headers);

        headers.clear();
        headers.put(HTTP_PATH, "/foo/bar");
        headers.put(REINDEXING_RECIPIENTS,
                "broker:queue:foo, broker:queue:bar,\t\nbroker:queue:foo   ");
        template.sendBodyAndHeaders("", headers);

        headers.clear();
        headers.put(HTTP_PATH, "/foo/bar");
        headers.put(REINDEXING_RECIPIENTS, null);
        template.sendBodyAndHeaders("", headers);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testRestProcessorWithBody1() throws Exception {
        final String body = "[\"broker:queue:foo\",\"broker:queue:bar\"]";

        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.message(0).header(REINDEXING_RECIPIENTS).contains("broker:queue:bar");
        resultEndpoint.message(0).header(REINDEXING_RECIPIENTS).contains("broker:queue:foo");

        final Map<String, Object> headers = new HashMap<>();
        headers.put(CONTENT_TYPE, "application/json");
        headers.put(HTTP_PATH, "/foo/bar");
        template.sendBodyAndHeaders(body, headers);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testRestProcessorWithBody2() throws Exception {
        final String body = "[\"broker:queue:foo\",\"broker:queue:bar\"]";

        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.message(0).header(REINDEXING_RECIPIENTS).contains("broker:queue:bar");
        resultEndpoint.message(0).header(REINDEXING_RECIPIENTS).contains("broker:queue:foo");
        resultEndpoint.message(0).header(REINDEXING_RECIPIENTS).contains("broker:queue:baz");

        final Map<String, Object> headers = new HashMap<>();
        headers.put(HTTP_PATH, "/foo/bar");
        headers.put(CONTENT_TYPE, "application/json");
        headers.put(REINDEXING_RECIPIENTS, "broker:queue:baz");
        template.sendBodyAndHeaders(body, headers);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testRestProcessorWithNullBody() throws Exception {

        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.message(0).header(REINDEXING_RECIPIENTS).isEqualTo("broker:queue:baz");

        final Map<String, Object> headers = new HashMap<>();
        headers.put(CONTENT_TYPE, "application/json");
        headers.put(HTTP_PATH, "/foo/bar");
        headers.put(REINDEXING_RECIPIENTS, "broker:queue:baz");
        template.sendBodyAndHeaders(null, headers);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testRestProcessorWithNoContentType() throws Exception {

        final String body = "[\"broker:queue:foo\",\"broker:queue:bar\"]";

        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.message(0).header(REINDEXING_RECIPIENTS).isEqualTo("broker:queue:baz");

        final Map<String, Object> headers = new HashMap<>();
        headers.put(HTTP_PATH, "/foo/bar");
        headers.put(REINDEXING_RECIPIENTS, "broker:queue:baz");
        template.sendBodyAndHeaders(body, headers);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testRestProcessorWithBadContentType() throws Exception {

        final String body = "[\"broker:queue:foo\",\"broker:queue:bar\"]";

        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.message(0).header(REINDEXING_RECIPIENTS).isEqualTo("broker:queue:baz");

        final Map<String, Object> headers = new HashMap<>();
        headers.put(CONTENT_TYPE, "application/foo");
        headers.put(HTTP_PATH, "/foo/bar");
        headers.put(REINDEXING_RECIPIENTS, "broker:queue:baz");
        template.sendBodyAndHeaders(body, headers);

        assertMockEndpointsSatisfied();
    }


    @Test
    public void testRestProcessorWithEmptyBody() throws Exception {

        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.message(0).header(REINDEXING_RECIPIENTS).isEqualTo("broker:queue:baz");

        final Map<String, Object> headers = new HashMap<>();
        headers.put(CONTENT_TYPE, "application/json");
        headers.put(HTTP_PATH, "/foo/bar");
        headers.put(REINDEXING_RECIPIENTS, "broker:queue:baz");
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
