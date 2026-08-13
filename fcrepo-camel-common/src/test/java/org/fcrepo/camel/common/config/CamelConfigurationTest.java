/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel.common.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spring.spi.CamelBeanPostProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tests the CamelConfiguration base class that replaces the removed camel-spring-javaconfig module.
 *
 * @author Dan Field
 */
public class CamelConfigurationTest {

    @Test
    public void buildsSpringManagedContextWithRegisteredRoutes() {
        try (var appContext = new AnnotationConfigApplicationContext(TestConfig.class)) {
            final CamelContext camelContext = appContext.getBean(CamelContext.class);
            assertNotNull(camelContext, "a Spring-managed CamelContext should be created");
            assertNotNull(camelContext.getRoute("test-route"),
                    "the RouteBuilder bean should be auto-registered on the context");

            final ProducerTemplate template = appContext.getBean(ProducerTemplate.class);
            assertNotNull(template, "a ProducerTemplate bean should be exposed");
            assertSame(camelContext, template.getCamelContext());

            assertNotNull(appContext.getBean(CamelBeanPostProcessor.class),
                    "a Camel bean post-processor should be registered for annotation injection");
        }
    }

    @Test
    public void exposesTheApplicationContext() {
        final var config = new TestConfig();
        final var appContext = new AnnotationConfigApplicationContext();
        config.setApplicationContext(appContext);
        assertSame(appContext, config.getApplicationContext());
        assertFalse(appContext.isActive());
        appContext.close();
    }

    @Configuration
    static class TestConfig extends CamelConfiguration {
        @Bean
        public RouteBuilder testRoute() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("direct:test").routeId("test-route").to("mock:end");
                }
            };
        }
    }
}
