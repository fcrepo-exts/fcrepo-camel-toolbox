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

    @Value("${fcrepo.cache.timeout:0}")
    private long fcrepoCacheTimeout;

    @Value("${rest.prefix:/ldpath}")
    private String restPrefix;

    @Value("${rest.host:localhost}")
    private String restHost;

    @Value("${rest.port:9085}")
    private int restPort;

    @Value("${cache.timeout:86400}")
    private int cacheTimeout;

    @Value("${ldcache.directory:ldcache/}")
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
