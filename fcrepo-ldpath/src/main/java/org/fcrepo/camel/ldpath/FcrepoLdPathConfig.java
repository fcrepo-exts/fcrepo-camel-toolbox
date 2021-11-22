/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel.ldpath;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.marmotta.ldcache.api.LDCachingBackend;
import org.apache.marmotta.ldcache.backend.file.LDCachingFileBackend;
import org.apache.marmotta.ldcache.model.CacheConfiguration;
import org.apache.marmotta.ldcache.services.LDCache;
import org.apache.marmotta.ldclient.api.endpoint.Endpoint;
import org.apache.marmotta.ldclient.api.provider.DataProvider;
import org.apache.marmotta.ldpath.api.functions.SelectorFunction;
import org.apache.marmotta.ldpath.backend.linkeddata.LDCacheBackend;
import org.fcrepo.camel.common.config.BasePropsConfig;
import org.fcrepo.client.FcrepoHttpClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static java.util.Collections.EMPTY_SET;

/**
 * A configuration class for the LDPath service
 *
 * @author dbernstein
 */
@Configuration
public class FcrepoLdPathConfig extends BasePropsConfig {

    @Value("${ldpath.fcrepo.cache.timeout:0}")
    private long fcrepoCacheTimeout;

    @Value("${ldpath.rest.prefix:/ldpath}")
    private String restPrefix;

    @Value("${ldpath.rest.host:localhost}")
    private String restHost;

    @Value("${ldpath.rest.port:9085}")
    private int restPort;

    @Value("${ldpath.cache.timeout:86400}")
    private int cacheTimeout;

    @Value("${ldpath.ldcache.directory:ldcache/}")
    private Path ldCacheDirectory;

    @Value("${ldpath.transform.path:classpath:org/fcrepo/camel/ldpath/default.ldpath}")
    private String ldpathTransformPath;

    public String getRestHost() {
        return restHost;
    }

    public int getRestPort() {
        return restPort;
    }

    public String getRestPrefix() {
        return restPrefix;
    }

    public String getLdpathTransformPath() {
        return ldpathTransformPath;
    }

    @Bean("ldpath")
    public LDPathWrapper ldpath() throws Exception {
        final var fcrepoBaseUrl = getFcrepoBaseUrl();
        final var fcrepoAuthHost = getFcrepoAuthHost();
        final var fcrepoUsername = getFcrepoUsername();
        final var fcrepoPassword = getFcrepoPassword();

        final AuthScope authScope;
        if (fcrepoAuthHost == null || fcrepoAuthHost.isBlank()) {
            authScope = new AuthScope(AuthScope.ANY);
        } else {
            authScope = new AuthScope(new HttpHost(fcrepoAuthHost));
        }
        final Credentials credentials = new UsernamePasswordCredentials(fcrepoUsername, fcrepoPassword);
        final List<Endpoint> endpoints = List.of(new FedoraEndpoint(fcrepoBaseUrl, fcrepoCacheTimeout));
        final var fcrepoHttpClientBuilder = new FcrepoHttpClientBuilder(fcrepoUsername, fcrepoPassword, fcrepoAuthHost);
        final List<DataProvider> providers = List.of(new FedoraProvider(fcrepoHttpClientBuilder));
        final var client = ClientFactory.createClient(authScope, credentials, endpoints, providers);
        final var config = new CacheConfiguration(client);
        config.setDefaultExpiry(cacheTimeout);
        final LDCachingBackend ldCachingBackend = new LDCachingFileBackend(ldCacheDirectory.toFile());
        ldCachingBackend.initialize();
        final LDCache ldCache = new LDCache(config, ldCachingBackend);
        final var backend = new LDCacheBackend(ldCache);
        return new LDPathWrapper(backend, createSelectorFunctions());
    }

    protected Set<SelectorFunction> createSelectorFunctions() {
        return EMPTY_SET;
    }

    @Bean
    public LDPathRouter ldPathRouter() {
        return new LDPathRouter();
    }

}
