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

import static java.util.Collections.emptyList;

import java.util.List;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.apache.marmotta.ldclient.api.endpoint.Endpoint;
import org.apache.marmotta.ldclient.api.provider.DataProvider;
import org.apache.marmotta.ldclient.endpoint.rdf.LinkedDataEndpoint;
import org.apache.marmotta.ldclient.model.ClientConfiguration;
import org.apache.marmotta.ldclient.provider.rdf.CacheProvider;
import org.apache.marmotta.ldclient.provider.rdf.LinkedDataProvider;
import org.apache.marmotta.ldclient.provider.rdf.RegexUriProvider;
import org.apache.marmotta.ldclient.provider.rdf.SPARQLProvider;

/**
 * A convenience factory for creating a ClientConfiguration object
 * @author acoburn
 * @since Aug 5, 2016
 */
public class ClientFactory {

    /**
     * Configure a linked data client suitable for use with a Fedora Repository.
     * @param fedoraEndpoint a FedoraEndpoint configuration
     * @return a configuration for use with an LDClient
     */
    public static ClientConfiguration createClient(final Endpoint fedoraEndpoint) {
        return createClient(null, null, fedoraEndpoint, emptyList(), emptyList());
    }

    /**
     * Configure a linked data client suitable for use with a Fedora Repository.
     * @param fedoraEndpoint a FedoraEndpoint configuration
     * @param endpoints additional endpoints to enable on the client
     * @param providers additional providers to enable on the client
     * @return a configuration for use with an LDClient
     */
    public static ClientConfiguration createClient(final Endpoint fedoraEndpoint, final List<Endpoint> endpoints,
            final List<DataProvider> providers) {
        return createClient(null, null, fedoraEndpoint, endpoints, providers);
    }

    /**
     * Configure a linked data client suitable for use with a Fedora Repository.
     * @param authScope the authentication scope
     * @param credentials the credentials
     * @param fedoraEndpoint a FedoraEndpoint configuration
     * @return a configuration for use with an LDClient
     */
    public static ClientConfiguration createClient(final AuthScope authScope, final Credentials credentials,
            final Endpoint fedoraEndpoint) {
        return createClient(authScope, credentials, fedoraEndpoint, emptyList(), emptyList());
    }

    /**
     * Create a linked data client suitable for use with a Fedora Repository.
     * @param authScope the authentication scope
     * @param credentials the credentials
     * @param fedoraEndpoint a FedoraEndpoint configuration
     * @param endpoints additional endpoints to enable on the client
     * @param providers additional providers to enable on the client
     * @return a configuration for use with an LDClient
     */
    public static ClientConfiguration createClient(final AuthScope authScope, final Credentials credentials,
            final Endpoint fedoraEndpoint, final List<Endpoint> endpoints, final List<DataProvider> providers) {

        final ClientConfiguration client = new ClientConfiguration();

        if (credentials != null && authScope != null) {
            final CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(authScope, credentials);
            client.setHttpClient(HttpClients.custom()
                    .setDefaultCredentialsProvider(credsProvider)
                    .useSystemProperties().build());
        }
        client.addEndpoint(fedoraEndpoint);

        // manually add default Providers and Endpoints
        client.addProvider(new LinkedDataProvider());
        client.addProvider(new CacheProvider());
        client.addProvider(new RegexUriProvider());
        client.addProvider(new SPARQLProvider());
        client.addEndpoint(new LinkedDataEndpoint());

        // add any injected endpoints/providers
        endpoints.forEach(client::addEndpoint);
        providers.forEach(client::addProvider);

        return client;
    }

    private ClientFactory() {
        // prevent instantiation
    }

}
