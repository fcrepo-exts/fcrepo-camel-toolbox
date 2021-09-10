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
package org.fcrepo.camel.indexing.solr;

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
@Conditional(FcrepoSolrIndexerConfig.SolrIndexerEnabled.class)
public class FcrepoSolrIndexerConfig extends BasePropsConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(FcrepoSolrIndexerConfig.class);
    static final String SOLR_INDEXER_ENABLED = "solr.indexer.enabled";

    static class SolrIndexerEnabled extends ConditionOnPropertyTrue {
        SolrIndexerEnabled() {
            super(FcrepoSolrIndexerConfig.SOLR_INDEXER_ENABLED, false);
        }
    }

    @Value("${fcrepo.checkHasIndexingTransformation:true}")
    private boolean checkHasIndexingTransformation;

    @Value("${fcrepo.defaultTransform:}")
    private String defaultTransform;

    @Value("${input.stream:broker:topic:fedora}")
    private String inputStream;

    @Value("${solr.reindex.stream:broker:queue:solr.reindex}")
    private String reindexStream;

    @Value("${solr.commitWithin:10000}")
    private long commitWithin;

    @Value("${indexing.predicate:false}")
    private boolean indexingPredicate;

    @Value("${ldpath.service.baseUrl:http://localhost:9085/ldpath}")
    private String ldpathServiceBaseUrl;

    @Value("${filter.containers:http://localhost:8080/fcrepo/rest/audit}")
    private String filterContainers;

    @Value("${solr.baseUrl:http://localhost:8983/solr/collection1}")
    private String solrBaseUrl;

    public boolean isCheckHasIndexingTransformation() {
        return checkHasIndexingTransformation;
    }

    public String getDefaultTransform() {
        return defaultTransform;
    }

    public String getInputStream() {
        return inputStream;
    }

    public String getReindexStream() {
        return reindexStream;
    }

    public long getCommitWithin() {
        return commitWithin;
    }

    public boolean isIndexingPredicate() {
        return indexingPredicate;
    }

    public String getLdpathServiceBaseUrl() {
        return ldpathServiceBaseUrl;
    }

    public String getFilterContainers() {
        return filterContainers;
    }

    public String getSolrBaseUrl() {
        return solrBaseUrl;
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
    public RouteBuilder solrRoute() {
        return new SolrRouter();
    }
}
