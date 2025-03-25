/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel.indexing.triplestore;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http.HttpComponent;
import org.fcrepo.camel.common.config.BasePropsConfig;
import org.fcrepo.camel.common.config.ConditionOnPropertyTrue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * A configuration class for the triplestore Indexing service
 *
 * @author dbernstein
 */
@Configuration
@Conditional(FcrepoTripleStoreIndexingConfig.TriplestoreIndexingEnabled.class)
public class FcrepoTripleStoreIndexingConfig extends BasePropsConfig {

    static final String TRIPLESTORE_INDEXING_ENABLED = "triplestore.indexing.enabled";

    static class TriplestoreIndexingEnabled extends ConditionOnPropertyTrue {
        TriplestoreIndexingEnabled() {
            super(FcrepoTripleStoreIndexingConfig.TRIPLESTORE_INDEXING_ENABLED, false);
        }
    }

    @Value("${triplestore.using.docuteam.model:true}")
    private boolean usingDocuteamModel;

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

    @Value("${triplestore.authUsername:}")
    private String triplestoreAuthUsername;

    @Value("${triplestore.authPassword:}")
    private String triplestoreAuthPassword;

    @Value("${triplestore.aggregator.completion.size:100}")
    private Integer triplestoreAggregatorCompletionSize;

    // In MS
    @Value("${triplestore.aggregator.completion.timeout:5000}")
    private Long triplestoreAggregatorCompletionTimeout;

    public boolean isUsingDocuteamModel() {
        return usingDocuteamModel;
    }

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

    public String getTriplestoreAuthUsername() {
        return triplestoreAuthUsername;
    }

    public String getTriplestoreAuthPassword() {
        return triplestoreAuthPassword;
    }

    public Integer getTriplestoreAggregatorCompletionSize() {
        return triplestoreAggregatorCompletionSize;
    }

    public Long getTriplestoreAggregatorCompletionTimeout() {
        return triplestoreAggregatorCompletionTimeout;
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
