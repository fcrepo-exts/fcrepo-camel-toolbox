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

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.spring.javaconfig.CamelConfiguration;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import static org.apache.camel.component.mock.MockEndpoint.assertIsSatisfied;
import static org.apache.camel.util.ObjectHelper.loadResourceAsStream;
import static org.fcrepo.camel.audit.triplestore.AuditSparqlProcessor.AUDIT;
import static org.fcrepo.camel.audit.triplestore.AuditSparqlProcessor.PREMIS;
import static org.junit.Assert.assertTrue;

/**
 * Test the route workflow.
 *
 * @author escowles
 * @author Aaron Coburn
 * @since 2015-04-10
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {RouteTest.ContextConfig.class}, loader = AnnotationConfigContextLoader.class)
public class RouteTest {

    @EndpointInject("mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce("direct:start")
    protected ProducerTemplate template;

    @Autowired
    private CamelContext camelContext;

    private static final String baseURL = "http://localhost/rest";
    private static final String fileID = "/file1";
    private static final String auditContainer = "/audit";
    private static final String eventDate = "2015-04-06T22:45:20Z";
    private static final String userID = "fedo raAdmin";
    private static final String userAgent = "CLAW client/1.0";


    @BeforeClass
    public static void beforeClass() {
        System.setProperty("audit.input.stream", "seda:foo");
        System.setProperty("audit.filter.containers", baseURL + auditContainer);
        System.setProperty("audit.enabled", "true");
    }

    @Test
    public void testWithoutJms() throws Exception {

        final var context = camelContext.adapt(ModelCamelContext.class);

        AdviceWith.adviceWith(context, "AuditFcrepoRouter", a -> {
            a.replaceFromWith("direct:start");
        });

        AdviceWith.adviceWith(context, "AuditEventRouter", a -> {
            a.mockEndpointsAndSkip("http*");
            a.weaveAddLast().to("mock:result");
        });

        resultEndpoint.expectedMessageCount(2);
        resultEndpoint.expectedHeaderReceived(Exchange.CONTENT_TYPE, "application/x-www-form-urlencoded");
        resultEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");
        resultEndpoint.expectedHeaderReceived(AuditHeaders.EVENT_BASE_URI, "http://example.com/event");

        template.sendBody(loadResourceAsStream("event_delete_binary.json"));
        template.sendBody(loadResourceAsStream("event_delete_resource.json"));
        template.sendBody(loadResourceAsStream("event_audit_resource.json"));
        template.sendBody(loadResourceAsStream("event_audit_update.json"));

        assertIsSatisfied(resultEndpoint);
        final String body = (String) resultEndpoint.assertExchangeReceived(0).getIn().getBody();
        assertTrue("Event type not found!",
                body.contains("<" + PREMIS + "hasEventType> <" + AUDIT + "contentRemoval>"));
        assertTrue("Object link not found!",
                body.contains("<" + PREMIS + "hasEventRelatedObject> <" + baseURL + fileID + ">"));
    }

    @Configuration
    @ComponentScan(basePackages = "org.fcrepo.camel.audit")
    static class ContextConfig extends CamelConfiguration {

    }
}
