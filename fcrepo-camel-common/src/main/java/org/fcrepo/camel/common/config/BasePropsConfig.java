/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.camel.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;


/**
 * A base class for property configs
 *
 * @author dbernstein
 */
@PropertySources({
        @PropertySource(value = BasePropsConfig.DEFAULT_FCREPO_CAMEL_TOOLBOX_CONFIG_FILE_PROP_SOURCE,
                ignoreResourceNotFound = true),
        @PropertySource(value = BasePropsConfig.FCREPO_CAMEL_CONFIG_FILE_PROP_SOURCE, ignoreResourceNotFound = true)
})
public abstract class BasePropsConfig {

    public static final String FCREPO_CAMEL_TOOLBOX_HOME_PROPERTY = "fcrepo-camel-toolbox.home";
    public static final String DEFAULT_FCREPO_HOME_VALUE = "fcrepo-camel-toolbox-home";
    public static final String DEFAULT_FCREPO_CAMEL_TOOLBOX_CONFIG_FILE_PROP_SOURCE =
            "file:${" + FCREPO_CAMEL_TOOLBOX_HOME_PROPERTY + ":" + DEFAULT_FCREPO_HOME_VALUE +
                    "}/config/fcrepo-camel-toolbox.properties";
    public static final String FCREPO_CAMEL_CONFIG_FILE_PROPERTY = "fcrepo.camel.toolbox.config.file";
    public static final String FCREPO_CAMEL_CONFIG_FILE_PROP_SOURCE =
            "file:${" + FCREPO_CAMEL_CONFIG_FILE_PROPERTY + "}";

    @Value("${error.maxRedeliveries:10}")
    private int maxRedeliveries;

    @Value("${fcrepo.baseUrl:http://localhost:8080/fcrepo/rest}")
    private String fcrepoBaseUrl;

    @Value("${fcrepo.authUsername:fedoraAdmin}")
    private String fcrepoUsername;

    @Value("${fcrepo.authPassword:fedoraAdmin}")
    private String fcrepoPassword;

    @Value("${fcrepo.authHost:localhost}")
    private String fcrepoAuthHost;

    public int getMaxRedeliveries() {
        return maxRedeliveries;
    }

    public String getFcrepoBaseUrl() {
        return fcrepoBaseUrl;
    }

    public String getFcrepoUsername() {
        return fcrepoUsername;
    }

    public String getFcrepoPassword() {
        return fcrepoPassword;
    }

    public String getFcrepoAuthHost() {
        return fcrepoAuthHost;
    }

}
