/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel.fixity;

import org.apache.camel.CamelContext;
import org.apache.camel.spring.javaconfig.CamelConfiguration;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import static org.junit.Assert.assertNull;

/**
 * Test that the route can be disabled.
 *
 * @author dbernstein
 * @since 2021-10-01
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {RouteDisabledTest.ContextConfig.class}, loader = AnnotationConfigContextLoader.class)
public class RouteDisabledTest {

    @Autowired
    private CamelContext camelContext;

    @Autowired(required = false)
    private FcrepoFixityConfig config;

    @BeforeClass
    public static void beforeClass() {
        System.setProperty("fixity.enabled", "false");
    }

    @DirtiesContext
    @Test
    public void testFixityDisabled() throws Exception {
       assertNull("fixity config should be null", config);
    }

    @Configuration
    @ComponentScan(resourcePattern = "**/Fcrepo*.class")
    static class ContextConfig extends CamelConfiguration {
    }

}

