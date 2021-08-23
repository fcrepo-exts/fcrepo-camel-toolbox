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
package org.fcrepo.camel.indexing.triplestore;

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
 * A configuration class for the Solr Indexer service
 *
 * @author dbernstein
 */
@Configuration
@Conditional(FcrepoTripleStoreIndexerConfig.TriplestoreIndexerEnabled.class)
public class FcrepoTripleStoreIndexerConfig extends BasePropsConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(FcrepoTripleStoreIndexerConfig.class);
    static final String TRIPLESTORE_INDEXER_ENABLED = "triplestore.indexer.enabled";

    static class TriplestoreIndexerEnabled extends ConditionOnPropertyTrue {
        TriplestoreIndexerEnabled() {
            super(FcrepoTripleStoreIndexerConfig.TRIPLESTORE_INDEXER_ENABLED, true);
        }
    }

    @Value("${triplestore.input.stream:broker:topic:fedora}")
    private String inputStream;

    @Value("${triplestore.reindex.stream:broker:queue:triplestore.reindex}")
    private String reindexStream;

    @Value("${triplestore.indexing.predicate:false}")
    private boolean indexingPredicate;

    @Value("${triplestore.namedGraph:}")
    private String namedGraph;

    @Value("${triplestore.filter.containers:http://localhost:8080/fcrepo/rest/audit}")
    private String filterContainers;

    @Value("${triplestore.prefer.include:}")
    private String preferInclude;

    @Value("${triplestore.prefer.omit:http://www.w3.org/ns/ldp#PreferContainment}")
    private String preferOmit;

    @Value("${triplestore.baseUrl:http://localhost:8080/fuseki/test/update}")
    private String triplestoreBaseUrl;

    public String getInputStream() {
        return inputStream;
    }

    public String getReindexStream() {
        return reindexStream;
    }

    public boolean isIndexingPredicate() {
        return indexingPredicate;
    }

    public String getNamedGraph() {
        return namedGraph;
    }

    public String getFilterContainers() {
        return filterContainers;
    }

    public String getPreferInclude() {
        return preferInclude;
    }

    public String getPreferOmit() {
        return preferOmit;
    }

    public String getTriplestoreBaseUrl() {
        return triplestoreBaseUrl;
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
    public RouteBuilder tripleStoreRoute() {
        return new TriplestoreRouter();
    }

}
