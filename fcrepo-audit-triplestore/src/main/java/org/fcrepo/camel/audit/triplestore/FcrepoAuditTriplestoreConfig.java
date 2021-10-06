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
package org.fcrepo.camel.audit.triplestore;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http.HttpComponent;
import org.fcrepo.camel.common.config.BasePropsConfig;
import org.fcrepo.camel.common.config.ConditionOnPropertyTrue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * A configuration class for the audit triplestore service
 *
 * @author dbernstein
 */
@Configuration
@Conditional({FcrepoAuditTriplestoreConfig.AuditTriplestoreEnabled.class})
public class FcrepoAuditTriplestoreConfig extends BasePropsConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(FcrepoAuditTriplestoreConfig.class);
    static final String AUDIT_ENABLED = "audit.enabled";

    static class AuditTriplestoreEnabled extends ConditionOnPropertyTrue {
        AuditTriplestoreEnabled() {
            super(FcrepoAuditTriplestoreConfig.AUDIT_ENABLED, false);
        }
    }

    @Value("${audit.input.stream:broker:topic:fedora}")
    private String inputStream;

    @Value("${audit.event.baseUri:http://example.com/event}")
    private String eventBaseUri;

    @Value("${audit.triplestore.baseUrl:http://localhost:3030/fuseki/test/update}")
    private String triplestoreBaseUrl;

    @Value("${audit.triplestore.authUsername:}")
    private String triplestoreAuthUsername;

    @Value("${audit.triplestore.authPassword:}")
    private String tripleStoreAuthPassword;

    @Value("${audit.filter.containers:http://localhost:8080/fcrepo/rest/audit}")
    private String filterContainers;

    @Bean(name = "http")
    public HttpComponent http() {
        return new HttpComponent();
    }

    @Bean(name = "https")
    public HttpComponent https() {
        return new HttpComponent();
    }

    @Bean
    public RouteBuilder route() {
        return new EventRouter();
    }

    public String getTriplestoreBaseUrl() {
        return triplestoreBaseUrl;
    }

    public String getTriplestoreAuthPassword() {
        return tripleStoreAuthPassword;
    }

    public String getTriplestoreAuthUsername() {
        return triplestoreAuthUsername;
    }

    public String getFilterContainers() {
        return filterContainers;
    }

    public String getEventBaseUri() {
        return eventBaseUri;
    }

    public String getInputStream() {
        return inputStream;
    }
}
