/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel.ldpath;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

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
     * @param endpoint  Endpoint to enable on the client
     * @return a configuration for use with an LDClient
     */
    public static ClientConfiguration createClient(final Endpoint endpoint) {
        return createClient(singletonList(endpoint), emptyList());
    }

    /**
     * Configure a linked data client suitable for use with a Fedora Repository.
     * @param provider Provider to enable on the client
     * @return a configuration for use with an LDClient
     */
    public static ClientConfiguration createClient(final DataProvider provider) {
        return createClient(null, null, emptyList(), singletonList(provider));
    }


    /**
     * Configure a linked data client suitable for use with a Fedora Repository.
     * @param endpoint  Endpoint to enable on the client
     * @param provider Provider to enable on the client
     * @return a configuration for use with an LDClient
     */
    public static ClientConfiguration createClient(final Endpoint endpoint, final DataProvider provider) {
        return createClient(singletonList(endpoint), singletonList(provider));
    }

    /**
     * Configure a linked data client suitable for use with a Fedora Repository.
     * @param endpoints additional endpoints to enable on the client
     * @param providers additional providers to enable on the client
     * @return a configuration for use with an LDClient
     */
    public static ClientConfiguration createClient(final List<Endpoint> endpoints, final List<DataProvider> providers) {
        return createClient(null, null, endpoints, providers);
    }

    /**
     * Configure a linked data client suitable for use with a Fedora Repository.
     * @param authScope the authentication scope
     * @param credentials the credentials
     * @return a configuration for use with an LDClient
     */
    public static ClientConfiguration createClient(final AuthScope authScope, final Credentials credentials) {
        return createClient(authScope, credentials, emptyList(), emptyList());
    }

    /**
     * Create a linked data client suitable for use with a Fedora Repository.
     * @param authScope the authentication scope
     * @param credentials the credentials
     * @param endpoints additional endpoints to enable on the client
     * @param providers additional providers to enable on the client
     * @return a configuration for use with an LDClient
     */
    public static ClientConfiguration createClient(final AuthScope authScope, final Credentials credentials,
            final List<Endpoint> endpoints, final List<DataProvider> providers) {

        final ClientConfiguration client = new ClientConfiguration();

        if (credentials != null && authScope != null) {
            final CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(authScope, credentials);
            client.setHttpClient(HttpClients.custom()
                    .setDefaultCredentialsProvider(credsProvider)
                    .useSystemProperties().build());
        }

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
