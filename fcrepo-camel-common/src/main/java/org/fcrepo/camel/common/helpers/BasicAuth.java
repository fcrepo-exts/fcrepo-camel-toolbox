/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.camel.common.helpers;

import java.util.Base64;

/**
 * Helper class to generate a Basic Auth Header
 *
 * @author Thomas Bernhart
 * @since 2021-10-11
 */
public final class BasicAuth {

    public static final String BASIC_AUTH_HEADER = "Authorization";

    private BasicAuth() {
        //intentionally left blank
    }
    public static final String generateBasicAuthHeader(final String username, final String password) {
        return "Basic " + Base64.getEncoder()
            .encodeToString((username + ":" + password).getBytes());
    }
}
