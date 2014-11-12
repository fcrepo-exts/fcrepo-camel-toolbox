/**
 * Copyright 2014 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.camel;

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.api.management.ManagedResource;
import org.apache.camel.api.management.ManagedAttribute;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;

/**
 * Represents a Fedora endpoint.
 * @author Aaron Coburn
 * @since October 20, 2014
 */
@ManagedResource(description = "Managed FcrepoEndpoint")
@UriEndpoint(scheme = "fcrepo")
public class FedoraEndpoint extends DefaultEndpoint {

    public static final String FCREPO_BASE_URL = "FCREPO_BASE_URL";

    public static final String FCREPO_IDENTIFIER = "FCREPO_IDENTIFIER";

    public static final String DEFAULT_CONTENT_TYPE = "application/rdf+xml";

    private volatile String baseUrl = "";

    @UriParam
    private volatile String contentType = null;

    @UriParam
    private volatile String accept = null;

    @UriParam
    private volatile String transform = null;

    @UriParam
    private volatile String authUsername = null;

    @UriParam
    private volatile String authPassword = null;

    @UriParam
    private volatile String authHost = null;

    @UriParam
    private volatile Boolean tombstone = false;

    @UriParam
    private volatile Boolean metadata = true;

    @UriParam
    private volatile Boolean throwExceptionOnFailure = true;

    /**
     * Create a FedoraEndpoint with a uri, path and component
     * @param uri the endpoint uri (without path values)
     * @param remaining any path values on the endpoint uri
     * @param component an existing component value
     */
    public FedoraEndpoint(final String uri, final String remaining, final FedoraComponent component) {
        super(uri, component);
        this.setBaseUrl(remaining);
    }

    /**
     * Create a producer endpoint.
     */
    @Override
    public Producer createProducer() {
        return new FedoraProducer(this);
    }

    /**
     * This component does not implement a consumer endpoint.
     */
    @Override
    public Consumer createConsumer(final Processor processor) {
        throw new RuntimeCamelException("Cannot produce to a FedoraEndpoint: " + getEndpointUri());
    }

    /**
     * Define the component as a singleton
     */
    @Override
    public boolean isSingleton() {
        return true;
    }

    /**
     * baseUrl setter
     * @param url the baseUrl string
     */
    public void setBaseUrl(final String url) {
        this.baseUrl = url;
    }

    /**
     * baseUrl getter
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * accept setter
     * @param type the content-type for Accept headers
     */
    @ManagedAttribute(description = "Accept: Header")
    public void setAccept(final String type) {
        this.accept = type.replaceAll(" ", "+");
    }

    /**
     * accept getter
     */
    @ManagedAttribute(description = "Accept: Header")
    public String getAccept() {
        return accept;
    }

    /**
     * contentType setter
     * @param type the content-type for Content-Type headers
     */
    @ManagedAttribute(description = "Content-Type: Header")
    public void setContentType(final String type) {
        this.contentType = type.replaceAll(" ", "+");
    }

    /**
     * contentType getter
     */
    @ManagedAttribute(description = "Content-Type: Header")
    public String getContentType() {
        return contentType;
    }

    /**
     * authUsername setter
     * @param username used for authentication
     */
    @ManagedAttribute(description = "Username for authentication")
    public void setAuthUsername(final String username) {
        this.authUsername = username;
    }

    /**
     * authUsername getter
     */
    @ManagedAttribute(description = "Username for authentication")
    public String getAuthUsername() {
        return authUsername;
    }

    /**
     * authPassword setter
     * @param password used for authentication
     */
    @ManagedAttribute(description = "Password for authentication")
    public void setAuthPassword(final String password) {
        this.authPassword = password;
    }

    /**
     * authPassword getter
     */
    @ManagedAttribute(description = "Password for authentication")
    public String getAuthPassword() {
        return authPassword;
    }

    /**
     * authHost setter
     * @param host used for authentication
     */
    @ManagedAttribute(description = "Hostname for authentication")
    public void setAuthHost(final String host) {
        this.authHost = host;
    }

    /**
     * authHost getter
     */
    @ManagedAttribute(description = "Hostname for authentication")
    public String getAuthHost() {
        return authHost;
    }

    /**
     * metadata setter
     * @param metadata whether to retrieve rdf metadata for non-rdf nodes
     */
    @ManagedAttribute(description = "Whether to retrieve the /fcr:metadata endpoint for Binary nodes")
    public void setMetadata(final String metadata) {
        this.metadata = Boolean.valueOf(metadata);
    }

    /**
     * metadata getter
     */
    @ManagedAttribute(description = "Whether to retrieve the /fcr:metadata endpoint for Binary nodes")
    public Boolean getMetadata() {
        return metadata;
    }

    /**
     * throwExceptionOnFailure setter
     * @param throwOnFailure whether non-2xx HTTP response codes throw exceptions
     */
    @ManagedAttribute(description = "Whether non 2xx response codes should throw an exception")
    public void setThrowExceptionOnFailure(final String throwOnFailure) {
        this.throwExceptionOnFailure = Boolean.valueOf(throwOnFailure);
    }

    /**
     * throwExceptionOnFailure getter
     */
    @ManagedAttribute(description = "Whether non 2xx response codes should throw an exception")
    public Boolean getThrowExceptionOnFailure() {
        return throwExceptionOnFailure;
    }

    /**
     * transform setter
     * @param transform define an LD-Path transform program for converting RDF to JSON
     */
    @ManagedAttribute(description = "The LDPath transform program to use")
    public void setTransform(final String transform) {
        this.transform = transform;
    }

    /**
     * transform getter
     */
    @ManagedAttribute(description = "The LDPath transform program to use")
    public String getTransform() {
        return transform;
    }

    /**
     * tombstone setter
     * @param tombstone whether to access the /fcr:tombstone endpoint for a resource
     */
    @ManagedAttribute(description = "Whether to use the /fcr:tombstone endpoint on objects")
    public void setTombstone(final String tombstone) {
        this.tombstone = Boolean.valueOf(tombstone);
    }

    /**
     * tombstone getter
     */
    @ManagedAttribute(description = "Whether to use the /fcr:tombstone endpoint on objects")
    public Boolean getTombstone() {
        return tombstone;
    }
}
