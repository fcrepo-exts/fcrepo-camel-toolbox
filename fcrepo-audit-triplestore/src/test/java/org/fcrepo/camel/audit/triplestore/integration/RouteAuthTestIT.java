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
package org.fcrepo.camel.audit.triplestore.integration;

import org.apache.camel.CamelContext;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.spring.javaconfig.CamelConfiguration;
import org.apache.jena.atlas.web.AuthScheme;
import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import static java.lang.Integer.parseInt;
import static org.apache.camel.util.ObjectHelper.loadResourceAsStream;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Test the event route using a triple store with authentication
 *
 * @since 2021-10-06
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class RouteAuthTestIT {

    final private Logger logger = getLogger(RouteAuthTestIT.class);

    private static FusekiServer server = null;

    @Produce("direct:start")
    protected ProducerTemplate template;

    @Autowired
    private CamelContext camelContext;

    private static final String baseURL = "http://localhost/rest";
    private static final String auditContainer = "/audit";

    private static final String FUSEKI_PORT = System.getProperty(
        "fuseki.dynamic.test.port", "8080"
    );

    @BeforeClass
    public static void beforeClass() {
        System.setProperty("audit.input.stream", "seda:foo");
        System.setProperty("audit.filter.containers", baseURL + auditContainer);
        System.setProperty("audit.enabled", "true");

        System.setProperty("audit.triplestore.baseUrl", "http://localhost:" + FUSEKI_PORT + "/fuseki/test/update");
        System.setProperty("audit.triplestore.authUsername", "admin");
        System.setProperty("audit.triplestore.authPassword", "password");
    }

    @After
    public void tearDownFuseki() throws Exception {
        logger.info("Stopping EmbeddedFusekiServer");
        server.stop();
    }

    @Before
    public void setUpFuseki() throws Exception {
        logger.info("Starting EmbeddedFusekiServer on port {}", FUSEKI_PORT);
        final Dataset ds = DatasetFactory.createTxnMem();
        server = FusekiServer.create()
            .verbose(true)
            .port(parseInt(FUSEKI_PORT))
            .contextPath("/fuseki")
            .add("/test", ds, true)
            .passwordFile("src/test/resources/passwd")
            .auth(AuthScheme.BASIC)
            .build();
        server.start();
    }

    @DirtiesContext
    @Test
    public void testBasicAuthFusekiShouldReceiveMessages() throws Exception {
        final String fusekiEndpoint = "mock:http:localhost:" + FUSEKI_PORT + "/fuseki/test/update";
        final var context = camelContext.adapt(ModelCamelContext.class);

        AdviceWith.adviceWith(context, "AuditFcrepoRouter", a -> {
            a.replaceFromWith("direct:start");
        });

        AdviceWith.adviceWith(context, "AuditEventRouter", a -> {
            a.mockEndpoints("*");
        });

        final var fusekiMockEndpoint = MockEndpoint.resolve(camelContext, fusekiEndpoint);
        fusekiMockEndpoint.expectedMessageCount(2);
        fusekiMockEndpoint.setAssertPeriod(5000);

        template.sendBody(loadResourceAsStream("event_delete_binary.json"));
        template.sendBody(loadResourceAsStream("event_delete_resource.json"));

        MockEndpoint.assertIsSatisfied(fusekiMockEndpoint);
    }

    @Configuration
    @ComponentScan(basePackages = "org.fcrepo.camel.audit")
    static class ContextConfig extends CamelConfiguration {

    }
}
