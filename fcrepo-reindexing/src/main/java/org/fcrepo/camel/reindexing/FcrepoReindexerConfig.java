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
@Conditional(FcrepoReindexerConfig.ReindexerEnabled.class)
public class FcrepoReindexerConfig extends BasePropsConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(FcrepoReindexerConfig.class);
    static final String REINDEXER_ENABLED = "reindexer.enabled";

    static class ReindexerEnabled extends ConditionOnPropertyTrue {
        ReindexerEnabled() {
            super(FcrepoReindexerConfig.REINDEXER_ENABLED, true);
        }
    }

    @Value("${reindexing.error.maxRedeliveries:10}")
    private int maxRedeliveries;

    @Value("${reindexing.stream:broker:queue:reindexing}")
    private String reindexingStream;

    @Value("${reindexing.rest.prefix:/reindexing}")
    private String restPrefix;

    @Value("${reindexing.rest.host:localhost}")
    private String restHost;

    @Value("${reindexing.rest.port:9080}")
    private int restPort;


    public int getMaxRedeliveries() {
        return maxRedeliveries;
    }

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
