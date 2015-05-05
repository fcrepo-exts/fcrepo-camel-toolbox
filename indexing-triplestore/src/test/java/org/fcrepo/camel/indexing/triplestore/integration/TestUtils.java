/**
 * Copyright 2015 DuraSpace, Inc.
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
package org.fcrepo.camel.indexing.triplestore.integration;

import java.io.InputStream;
import java.net.URLEncoder;
import java.util.concurrent.Callable;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.camel.Exchange;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

/**
 * Utility functions for the integration tests
 *
 * @author acoburn
 * @since 2015-04-21
 */
public class TestUtils {

    /**
     *  Format a Sparql-update for the provided subject.
     */
    public static String sparqlUpdate(final String uri) {
        final String rdf = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
        final String fcrepo = "http://fedora.info/definitions/v4/repository#";
        final String uuid = "278bad29-b6c9-4574-a921-00d822cd65da";

        return "update=INSERT DATA { " +
            "<" + uri + "> <" + rdf + "type> <http://www.w3.org/ns/ldp#RDFSource> . " +
            "<" + uri + "> <" + rdf + "type> <" + fcrepo + "Resource> . " +
            "<" + uri + "> <" + rdf + "type> <http://www.w3.org/ns/ldp#Container> . " +
            "<" + uri + "> <" + rdf + "type> <" + fcrepo + "Container> . " +
            "<" + uri + "> <" + fcrepo + "uuid> \"" + uuid + "\"^^<http://www.w3.org/2001/XMLSchema#string> . }";
    }

    /**
     * Get data from the provided URL
     */
    public static InputStream httpGet(final String url) throws Exception {
        final CloseableHttpClient httpClient = HttpClients.createDefault();
        final HttpGet get = new HttpGet(url);
        return httpClient.execute(get).getEntity().getContent();
    }

    /**
     * Post data to the provided URL
     */
    public static void httpPost(final String url, final String content, final String mimeType) throws Exception {
        final CloseableHttpClient httpClient = HttpClients.createDefault();
        final HttpPost post = new HttpPost(url);
        post.addHeader(Exchange.CONTENT_TYPE, mimeType);
        post.setEntity(new StringEntity(content));
        httpClient.execute(post);
    }

    /**
     * Populate fuseki with some triples for the given subject
     */
    public static void populateFuseki(final String fusekiBase, final String subject) throws Exception {
        httpPost(fusekiBase + "/update",
                sparqlUpdate(subject),
                "application/x-www-form-urlencoded");
    }

    /**
     * get a count of the items in the triplestore, corresponding to a given subject.
     */
    public static Callable<Integer> triplestoreCount(final String fusekiBase, final String subject)  throws Exception {
        final String query = "SELECT (COUNT(*) AS ?n) WHERE { <" + subject + "> ?o ?p . }";
        final String url = fusekiBase + "/query?query=" + URLEncoder.encode(query, "UTF-8") + "&output=json";
        final ObjectMapper mapper = new ObjectMapper();
        return new Callable<Integer>() {
            public Integer call() throws Exception {
                return Integer.valueOf(mapper.readTree(httpGet(url))
                        .get("results").get("bindings").get(0).get("n").get("value").asText(), 10);
            }
        };
    }

    private TestUtils() {
        // prevent instantiation
    }
}
