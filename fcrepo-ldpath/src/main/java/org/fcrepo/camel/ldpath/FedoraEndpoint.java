/*
 * Copyright 2016 DuraSpace, Inc.
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
package org.fcrepo.camel.ldpath;

import org.apache.marmotta.commons.http.ContentType;
import org.apache.marmotta.ldclient.api.endpoint.Endpoint;

/**
 * @author acoburn
 */
public class FedoraEndpoint extends Endpoint {

    /**
     * Create a Fedora endpoint to be used with the Marmotta LDClient
     * Note: the baseUrl defined here is used by the LDClient to determine
     * what rules to apply when dereferencing URIs that start with the
     * given namespace.
     *
     * @param namespace the namespace for the Fedora endpoint (e.g. http://localhost:8080/fcrepo/rest/)
     */
    public FedoraEndpoint(final String namespace) {
        this(namespace, 0L);
    }

    /**
     * Create a Fedora endpoint to be used with the Marmotta LDClient
     * Note: the baseUrl defined here is used by the LDClient to determine
     * what rules to apply when dereferencing URIs that start with the
     * given namespace.
     *
     * @param namespace the namespace for the Fedora endpoint (e.g. http://localhost:8080/fcrepo/rest/)
     * @param timeout the length of time (in seconds) to cache the data
     */
    public FedoraEndpoint(final String namespace, final Long timeout) {
        super("Fedora Repository", "Linked Data", namespace + ".*", null, timeout);
        setPriority(PRIORITY_HIGH);
        addContentType(new ContentType("application", "rdf+xml", 0.8));
        addContentType(new ContentType("text", "turtle", 1.0));
        addContentType(new ContentType("text", "n3", 0.8));
        addContentType(new ContentType("application", "ld+json", 0.5));
    }
}
