/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel.fixity;

import org.apache.camel.CamelContext;
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

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Test that the route can be disabled.
 *
 * @author dbernstein
 * @since 2021-10-01
 */
@CamelSpringTest
@ContextConfiguration(classes = {RouteDisabledTest.ContextConfig.class}, loader = AnnotationConfigContextLoader.class)
public class RouteDisabledTest {

    @Autowired
    private CamelContext camelContext;

    @Autowired(required = false)
    private FcrepoFixityConfig config;

    @BeforeAll
    public static void beforeClass() {
        System.setProperty("fixity.enabled", "false");
    }

    @DirtiesContext
    @Test
    public void testFixityDisabled() throws Exception {
       assertNull(config, "fixity config should be null");
    }

    @Configuration
    @ComponentScan(resourcePattern = "**/Fcrepo*.class")
    static class ContextConfig extends CamelConfiguration {
    }

}

