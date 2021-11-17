/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel.ldpath;

import org.apache.marmotta.commons.http.ContentType;
import org.apache.marmotta.ldclient.api.endpoint.Endpoint;

/**
 * @author acoburn
 * @since Aug 5, 2016
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
        super("Fedora Repository", FedoraProvider.PROVIDER_NAME, namespace + ".*", null, timeout);
        setPriority(PRIORITY_HIGH);
        addContentType(new ContentType("application", "rdf+xml", 0.8));
        addContentType(new ContentType("text", "turtle", 1.0));
        addContentType(new ContentType("text", "n3", 0.8));
        addContentType(new ContentType("application", "ld+json", 0.5));
    }
}
