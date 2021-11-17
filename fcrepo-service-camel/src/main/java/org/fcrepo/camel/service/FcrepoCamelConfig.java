/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel.service;

import org.fcrepo.camel.FcrepoComponent;
import org.fcrepo.camel.common.config.BasePropsConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author dbernstein
 */
@Configuration
public class FcrepoCamelConfig extends BasePropsConfig {

    @Bean("fcrepo")
    public FcrepoComponent fcrepoComponent() {
        final var fcrepoComponent = new FcrepoComponent();
        fcrepoComponent.setBaseUrl(getFcrepoBaseUrl());
        fcrepoComponent.setAuthUsername(getFcrepoUsername());
        fcrepoComponent.setAuthPassword(getFcrepoPassword());
        fcrepoComponent.setAuthHost(getFcrepoAuthHost());
        return fcrepoComponent;
    }

}

