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
import org.apache.camel.impl.DefaultEndpoint;

/**
 * Represents a Fedora endpoint.
 * @author Aaron Coburn
 * @since October 20, 2014
 */
public class FedoraEndpoint extends DefaultEndpoint {

    public static final String JMS_HEADER_PREFIX = "org.fcrepo.jms.";

    public static final String FCREPO_JMS_IDENTIFIER = JMS_HEADER_PREFIX + "identifier";

    public static final String FCREPO_IDENTIFIER = "FCREPO_IDENTIFIER";

    public static final String DEFAULT_CONTENT_TYPE = "application/rdf+xml";

    private volatile String contentType = null;

    private volatile String accept = null;

    private volatile String baseUrl = "";

    private volatile String transform = null;

    private volatile String authUsername = null;

    private volatile String authPassword = null;

    private volatile String authHost = null;

    private volatile Boolean tombstone = false;

    private volatile Boolean metadata = true;

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
    public Consumer createConsumer(final Processor processor) throws Exception {
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
    public void setAccept(final String type) {
        this.accept = type.replaceAll(" ", "+");
    }

    /**
     * accept getter
     */
    public String getAccept() {
        return accept;
    }

    /**
     * contentType setter
     * @param type the content-type for Content-Type headers
     */
    public void setContentType(final String type) {
        this.contentType = type.replaceAll(" ", "+");
    }

    /**
     * contentType getter
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * authUsername setter
     * @param username used for authentication
     */
    public void setAuthUsername(final String username) {
        this.authUsername = username;
    }

    /**
     * authUsername getter
     */
    public String getAuthUsername() {
        return authUsername;
    }

    /**
     * authPassword setter
     * @param password used for authentication
     */
    public void setAuthPassword(final String password) {
        this.authPassword = password;
    }

    /**
     * authPassword getter
     */
    public String getAuthPassword() {
        return authPassword;
    }

    /**
     * authHost setter
     * @param host used for authentication
     */
    public void setAuthHost(final String host) {
        this.authHost = host;
    }

    /**
     * authHost getter
     */
    public String getAuthHost() {
        return authHost;
    }

    /**
     * metadata setter
     * @param metadata whether to retrieve rdf metadata for non-rdf nodes
     */
    public void setMetadata(final String metadata) {
        this.metadata = Boolean.valueOf(metadata);
    }

    /**
     * metadata getter
     */
    public Boolean getMetadata() {
        return metadata;
    }

    /**
     * throwExceptionOnFailure setter
     * @param throwOnFailure whether non-2xx HTTP response codes throw exceptions
     */
    public void setThrowExceptionOnFailure(final String throwOnFailure) {
        this.throwExceptionOnFailure = Boolean.valueOf(throwOnFailure);
    }

    /**
     * throwExceptionOnFailure getter
     */
    public Boolean getThrowExceptionOnFailure() {
        return throwExceptionOnFailure;
    }

    /**
     * transform setter
     * @param transform define an LD-Path transform program for converting RDF to JSON
     */
    public void setTransform(final String transform) {
        this.transform = transform;
    }

    /**
     * transform getter
     */
    public String getTransform() {
        return transform;
    }

    /**
     * tombstone setter
     * @param tombstone whether to access the /fcr:tombstone endpoint for a resource
     */
    public void setTombstone(final String tombstone) {
        this.tombstone = Boolean.valueOf(tombstone);
    }

    /**
     * tombstone getter
     */
    public Boolean getTombstone() {
        return tombstone;
    }
}
