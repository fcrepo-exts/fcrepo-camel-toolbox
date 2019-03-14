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

import static org.slf4j.LoggerFactory.getLogger;

import org.apache.marmotta.ldclient.api.endpoint.Endpoint;
import org.apache.marmotta.ldclient.provider.rdf.LinkedDataProvider;
import org.fcrepo.client.FcrepoHttpClientBuilder;
import org.fcrepo.client.FcrepoLink;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.slf4j.Logger;


/**
 * An extension Linked Data provider to support Binary nodes in Fedora.
 *
 * @author Mohamed Mohideen Abdul Rasheed
 */
public class FedoraProvider extends LinkedDataProvider {

    private static final Logger LOGGER = getLogger(FedoraProvider.class);

    public static final String PROVIDER_NAME = "Fedora";

    private final HttpClient httpClient;

    private final String NON_RDF_SOURCE_URI = "http://www.w3.org/ns/ldp#NonRDFSource";

    /**
     * FedoraProvider
     * @param builder FcrepoHttpClientBuilder for building HttpClient
     */
    public FedoraProvider(final FcrepoHttpClientBuilder builder) {
        Objects.requireNonNull(builder);
        httpClient = builder.build();
    }

    /*
     * Return the name of this data provider. To be used e.g. in the configuration and in log messages.
     */
    @Override
    public String getName() {
        return PROVIDER_NAME;
    }

    /*
     * Return the describedBy URL for NonRdfSource resources. Return the resourseUri otherwise.
     */
    @Override
    public List<String> buildRequestUrl(final String resourceUri, final Endpoint endpoint) {
        LOGGER.debug("Processing: " + resourceUri);
        Objects.requireNonNull(resourceUri);
        try {
            final Optional<String> nonRdfSourceDescUri =
                    getNonRDFSourceDescribedByUri(resourceUri);
            if ( nonRdfSourceDescUri.isPresent() ) {
                return Collections.singletonList(nonRdfSourceDescUri.get());
            }
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
        return Collections.singletonList(resourceUri);
    }

    /*
    * Get the describedBy Uri if the resource has a NON_RDF_SOURCE_URI link header.
    */
    private Optional<String> getNonRDFSourceDescribedByUri(final String resourceUri) throws IOException {
        Optional<String> nonRdfSourceDescUri = Optional.empty();
        final Header[] links = getLinkHeaders(resourceUri);
        if ( links != null ) {
            String descriptionUri = null;
            boolean isNonRDFSource = false;
            for ( final Header h : links ) {
                final FcrepoLink link = new FcrepoLink(h.getValue());
                if ( link.getRel().equals("describedby") ) {
                    descriptionUri = link.getUri().toString();
                } else if ( link.getUri().toString().contains(NON_RDF_SOURCE_URI)) {
                    isNonRDFSource = true;
                }
            }
            LOGGER.debug("isNonRDFSource: " + isNonRDFSource);
            if (isNonRDFSource && descriptionUri != null) {
                nonRdfSourceDescUri = Optional.of(descriptionUri);
            }
        }
        return nonRdfSourceDescUri;
    }

    /*
    * Get the link headers for the resource at the given Uri.
    */
    private Header[] getLinkHeaders(final String resourceUri) throws IOException {
        final HttpHead request = new HttpHead(resourceUri);
        final HttpResponse response = httpClient.execute(request);
        LOGGER.debug("Got: " + response.getStatusLine().getStatusCode() + " for HEAD " + resourceUri);
        return response.getHeaders("Link");
    }

}
