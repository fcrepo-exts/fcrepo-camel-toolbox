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
package org.fcrepo.camel.integration;

import static java.lang.Integer.parseInt;
import static java.lang.System.getProperty;

/**
 * Utility functions for integration testing
 * @author Aaron Coburn
 * @since November 7, 2014
 */
public final class FedoraTestUtils {

    private static final int FCREPO_PORT = parseInt(getProperty(
                "test.port", "8080"));

    /**
     * This is a utility class; the constructor is off-limits
     */
    private FedoraTestUtils() {
    }

    /**
     * Retrieve the baseUrl for the fcrepo instance
     */
    public static String getFcrepoBaseUrl() {
        if (FCREPO_PORT == 80) {
            return "http://localhost/rest";
        }
        return "http://localhost:" + FCREPO_PORT + "/rest";
    }

    /**
     * Retrieve the endpoint uri for fcrepo
     */
    public static String getFcrepoEndpointUri() {
        if (FCREPO_PORT == 80) {
            return "fcrepo://localhost/rest";
        }
        return "fcrepo://localhost:" + FCREPO_PORT + "/rest";
    }

    /**
     * Retrieve an RDF document serialized in TTL
     */
    public static String getTurtleDocument() {
        return "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n\n" +
                "<> dc:title \"some title\" .";
    }

    /**
     * Retrieve an N3 document
     */
    public static String getN3Document() {
        return "<http://localhost/rest/path/a/b/c> <http://purl.org/dc/elements/1.1/author> \"Author\" .\n" +
                "<http://localhost/rest/path/a/b/c> <http://purl.org/dc/elements/1.1/title> \"Title\" .";
    }

    /**
     * Retrieve a simple text document
     */
    public static String getTextDocument() {
        return "Simple plain text document";
    }

    /**
     * Retrieve a sparql-update document
     */
    public static String getPatchDocument() {
        return "PREFIX dc: <http://purl.org/dc/elements/1.1/> \n\n" +
                "INSERT { <> dc:title \"another title\" . } \nWHERE { }";
    }
}
