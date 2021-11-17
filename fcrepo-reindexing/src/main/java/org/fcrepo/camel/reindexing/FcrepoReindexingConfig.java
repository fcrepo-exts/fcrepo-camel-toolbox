/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel.reindexing;

import org.apache.camel.builder.RouteBuilder;
import org.fcrepo.camel.common.config.BasePropsConfig;
import org.fcrepo.camel.common.config.ConditionOnPropertyTrue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * A configuration class for the re-indexer service
 *
 * @author dbernstein
 */

@Configuration
@Conditional(FcrepoReindexingConfig.ReindexingEnabled.class)
public class FcrepoReindexingConfig extends BasePropsConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(FcrepoReindexingConfig.class);
    static final String REINDEXING_ENABLED = "reindexing.enabled";

    static class ReindexingEnabled extends ConditionOnPropertyTrue {
        ReindexingEnabled() {
            super(FcrepoReindexingConfig.REINDEXING_ENABLED, true);
        }
    }

    @Value("${reindexing.stream:broker:queue:reindexing}")
    private String reindexingStream;

    @Value("${reindexing.rest.prefix:/reindexing}")
    private String restPrefix;

    @Value("${reindexing.rest.host:localhost}")
    private String restHost;

    @Value("${reindexing.rest.port:9080}")
    private int restPort;


    public String getReindexingStream() {
        return reindexingStream;
    }

    public String getRestPrefix() {
        return restPrefix;
    }

    public String getRestHost() {
        return restHost;
    }

    public int getRestPort() {
        return restPort;
    }

    @Bean
    public RouteBuilder reindexingRoute() {
        return new ReindexingRouter();
    }
}
