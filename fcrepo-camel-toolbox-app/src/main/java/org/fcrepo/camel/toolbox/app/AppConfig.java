/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.camel.toolbox.app;


import org.apache.camel.spring.javaconfig.CamelConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * A configuration class for the application
 *
 * @author dbernstein
 */
@Configuration
@ComponentScan(basePackages = {"org.fcrepo.camel"})
public class AppConfig extends CamelConfiguration {


}
