/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
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
import org.fcrepo.camel.common.config.CamelConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.apache.camel.test.spring.junit5.CamelSpringTest;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import static org.apache.camel.component.mock.MockEndpoint.assertIsSatisfied;
import static org.apache.camel.util.ObjectHelper.loadResourceAsStream;
import static org.fcrepo.camel.audit.triplestore.AuditSparqlProcessor.AUDIT;
import static org.fcrepo.camel.audit.triplestore.AuditSparqlProcessor.PREMIS;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test the route workflow.
 *
 * @author escowles
 * @author Aaron Coburn
 * @author  dbernstein
 * @since 2015-04-10
 */
@CamelSpringTest
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


    @BeforeAll
    public static void beforeClass() {
        System.setProperty("audit.input.stream", "seda:foo");
        System.setProperty("audit.filter.containers", baseURL + auditContainer);
        System.setProperty("audit.enabled", "true");
    }

    @DirtiesContext
    @Test
    public void testWithoutJms() throws Exception {

        final var context = ((ModelCamelContext) camelContext);

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

        assertIsSatisfied(resultEndpoint);
        final String body = (String) resultEndpoint.assertExchangeReceived(0).getIn().getBody();
        assertTrue(body.contains("<" + PREMIS + "hasEventType> <" + AUDIT + "contentRemoval>"),
                        "Event type not found!");
        assertTrue(body.contains("<" + PREMIS + "hasEventRelatedObject> <" + baseURL + fileID + ">"),
                        "Object link not found!");
    }

    @DirtiesContext
    @Test
    public void testFilterContainersWithoutJms() throws Exception {

        final var context = ((ModelCamelContext) camelContext);

        resultEndpoint.expectedMessageCount(0);
        resultEndpoint.setAssertPeriod(1000);

        AdviceWith.adviceWith(context, "AuditFcrepoRouter", a -> {
            a.replaceFromWith("direct:start");
        });

        AdviceWith.adviceWith(context, "AuditEventRouter", a -> {
            a.mockEndpointsAndSkip("http*");
            a.weaveAddLast().to("mock:result");
        });

        //send events that should be filtered
        template.sendBody(loadResourceAsStream("event_audit_resource.json"));
        template.sendBody(loadResourceAsStream("event_audit_update.json"));

        assertIsSatisfied(resultEndpoint);
    }

    @Configuration
    @ComponentScan(basePackages = "org.fcrepo.camel.audit")
    static class ContextConfig extends CamelConfiguration {

    }
}
