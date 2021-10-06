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
package org.fcrepo.camel.indexing.http;

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
@Conditional(FcrepoHttpIndexerConfig.HttpIndexerEnabled.class)
public class FcrepoHttpIndexerConfig extends BasePropsConfig {

    static final String HTTP_INDEXER_ENABLED = "http.indexer.enabled";

    static class HttpIndexerEnabled extends ConditionOnPropertyTrue {
        HttpIndexerEnabled() {
            super(FcrepoHttpIndexerConfig.HTTP_INDEXER_ENABLED, false);
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
