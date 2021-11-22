/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel.httpforwarding;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http.HttpComponent;
import org.fcrepo.camel.common.config.BasePropsConfig;
import org.fcrepo.camel.common.config.ConditionOnPropertyTrue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * A configuration class for the Http Indexer service
 *
 * @author Geoff Scholl
 * @author Demian Katz
 */
@Configuration
@Conditional(FcrepoHttpForwardingConfig.HttpForwardingingEnabled.class)
public class FcrepoHttpForwardingConfig extends BasePropsConfig {

    static final String HTTP_FORWARDING_ENABLED = "http.enabled";

    static class HttpForwardingingEnabled extends ConditionOnPropertyTrue {
        HttpForwardingingEnabled() {
            super(FcrepoHttpForwardingConfig.HTTP_FORWARDING_ENABLED, false);
        }
    }

    @Value("${http.input.stream:broker:topic:fedora}")
    private String inputStream;

    @Value("${http.reindex.stream:broker:queue:http.reindex}")
    private String reindexStream;

    @Value("${http.filter.containers:http://localhost:8080/fcrepo/rest/audit}")
    private String filterContainers;

    @Value("${http.baseUrl:}")
    private String httpBaseUrl;

    @Value("${http.authUsername:}")
    private String httpAuthUsername;

    @Value("${http.authPassword:}")
    private String httpAuthPassword;

    public String getInputStream() {
        return inputStream;
    }

    public String getReindexStream() {
        return reindexStream;
    }

    public String getFilterContainers() {
        return filterContainers;
    }

    public String getHttpBaseUrl() {
        return httpBaseUrl;
    }

    public String getHttpAuthUsername() {
        return httpAuthUsername;
    }

    public String getHttpAuthPassword() {
        return httpAuthPassword;
    }


    @Bean(name = "http")
    public HttpComponent http() {
        return new HttpComponent();
    }

    @Bean(name = "https")
    public HttpComponent https() {
        return new HttpComponent();
    }

    @Bean
    public RouteBuilder httpRoute() {
        return new HttpRouter();
    }
}
