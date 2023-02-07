/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
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
@Conditional(FcrepoSolrIndexingConfig.SolrIndexingEnabled.class)
public class FcrepoSolrIndexingConfig extends BasePropsConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(FcrepoSolrIndexingConfig.class);
    static final String SOLR_INDEXING_ENABLED = "solr.indexing.enabled";

    static class SolrIndexingEnabled extends ConditionOnPropertyTrue {
        SolrIndexingEnabled() {
            super(FcrepoSolrIndexingConfig.SOLR_INDEXING_ENABLED, false);
        }
    }

    @Value("${solr.fcrepo.checkHasIndexingTransformation:true}")
    private boolean checkHasIndexingTransformation;

    @Value("${solr.fcrepo.defaultTransform:}")
    private String defaultTransform;

    @Value("${solr.input.stream:broker:topic:fedora}")
    private String inputStream;

    @Value("${solr.reindex.stream:broker:queue:solr.reindex}")
    private String reindexStream;

    @Value("${solr.commitWithin:10000}")
    private long commitWithin;

    @Value("${solr.indexing.predicate:false}")
    private boolean indexingPredicate;

    @Value("${solr.filter.containers:http://localhost:8080/fcrepo/rest/audit}")
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
