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
package org.fcrepo.camel.indexing.solr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.fcrepo.camel.JmsHeaders;
import org.fcrepo.camel.FcrepoHeaders;

import org.junit.Test;

/**
 * Test the route workflow.
 *
 * @author Aaron Coburn
 * @since 2015-04-10
 */
public class DeleteProcessorTest extends CamelTestSupport {

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    @Test
    public void testDeleteProcessor() throws Exception {

        final String id = "/foo";
        final String baseUrl = "http://localhost/rest";

        resultEndpoint.expectedMessageCount(2);
        resultEndpoint.expectedHeaderReceived(FcrepoHeaders.FCREPO_IDENTIFIER, id);
        resultEndpoint.expectedHeaderReceived(FcrepoHeaders.FCREPO_BASE_URL, baseUrl);
        resultEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");
        resultEndpoint.expectedHeaderReceived(Exchange.CONTENT_TYPE, "application/json");

        final Map<String, Object> headers = new HashMap<>();
        headers.put(FcrepoHeaders.FCREPO_IDENTIFIER, id);
        headers.put(FcrepoHeaders.FCREPO_BASE_URL, baseUrl);

        template.sendBodyAndHeaders("", headers);

        headers.clear();
        headers.put(JmsHeaders.IDENTIFIER, id);
        headers.put(JmsHeaders.BASE_URL, baseUrl);
        template.sendBodyAndHeaders("", headers);

        final ObjectMapper mapper = new ObjectMapper();

        for (final Exchange e : resultEndpoint.getExchanges()) {
            final JsonNode root = mapper.readTree(e.getIn().getBody(String.class));
            final JsonNode deleteNode = root.get("delete");
            assertNotNull(deleteNode);
            assertNotNull(deleteNode.get("id"));
            assertEquals(deleteNode.get("id").asText(), baseUrl + id);
        }

        assertMockEndpointsSatisfied();
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() throws IOException {
                from("direct:start")
                    .process(new SolrDeleteProcessor())
                    .to("mock:result");
            }
        };
    }
}
