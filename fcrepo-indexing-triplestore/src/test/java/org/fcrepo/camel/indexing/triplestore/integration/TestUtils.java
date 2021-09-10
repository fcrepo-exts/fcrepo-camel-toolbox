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
import org.fcrepo.client.FcrepoClient;

import static org.fcrepo.client.FcrepoClient.client;

/**
 * Utility functions for the integration tests
 *
 * @author acoburn
 * @since 2015-04-21
 */
public class TestUtils {

    private static String FEDORA_USERNAME = "fedoraAdmin";
    private static String FEDORA_PASSWORD = "fedoraAdmin";

    /**
     *  Format a Sparql-update for the provided subject.
     *
     * @param uri string containing sparql-update
     * @return string containing formatted sparql-update
     */
    public static String sparqlUpdate(final String uri) {
        final String rdf = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
        final String fcrepo = "http://fedora.info/definitions/v4/repository#";

        return "update=INSERT DATA { " +
            "<" + uri + "> <" + rdf + "type> <http://www.w3.org/ns/ldp#RDFSource> . " +
            "<" + uri + "> <" + rdf + "type> <" + fcrepo + "Resource> . " +
            "<" + uri + "> <" + rdf + "type> <http://www.w3.org/ns/ldp#Container> . " +
            "<" + uri + "> <" + rdf + "type> <" + fcrepo + "Container> . }";
    }

    /**
     * Get data from the provided URL
     *
     * @param url string containing url
     * @return InputStream containing data
     * @throws Exception in the event of an HTTP client failure
     */
    public static InputStream httpGet(final String url) throws Exception {
        final CloseableHttpClient httpClient = HttpClients.createDefault();
        final HttpGet get = new HttpGet(url);
        return httpClient.execute(get).getEntity().getContent();
    }

    /**
     * Get an event for a given URL
     *
     * @param subject the URL
     * @param eventType the event type
     * @return the event message as JSON
     */
    public static String getEvent(final String subject, final String eventType) {
        return "{\n" +
                "\"id\": \"urn:uuid:3c834a8f-5638-4412-aa4b-35ea80416a18\", \n" +
                "\"type\" : [ \"http://www.w3.org/ns/prov#Activity\" ,\n" +
                "             \"" + eventType + "\" ],\n" +
                "\"name\": \"resource event\",\n" +
                "        \"published\": \"2016-05-19T17:17:43-04:00Z\",\n" +
                "        \"actor\": [{\n" +
                "   \"type\": [\"Person\"],\n" +
                "    \"id\": \"info:fedora/fedoraAdmin\"\n" +
                "}, {\n" +
                "    \"type\": [\"Application\"],\n" +
                "    \"name\": \"CLAW client/1.0\"\n" +
                "}],\n" +
                "        \n" +
                "\"object\" : {\n" +
                "    \"id\" : \"" + subject + "\" ,\n" +
                "            \"type\" : [\n" +
                "    \"http://www.w3.org/ns/prov#Entity\" ,\n" +
                "            \"http://fedora.info/definitions/v4/repository#Resource\" ,\n" +
                "            \"http://fedora.info/definitions/v4/repository#Container\" ,\n" +
                "            \"http://www.w3.org/ns/ldp#RDFSource\",\n" +
                "           \"http://www.w3.org/ns/ldp#BasicContainer\" ],\n" +
                "    \"isPartOf\" : \"http://localhost/rest\"\n" +
                "},\n" +
                " \n" +
                "\"@context\": [\"https://www.w3.org/ns/activitystreams\", {\n" +
                "       \"prov\": \"http://www.w3.org/ns/prov#\",\n" +
                "        \"dcterms\": \"http://purl.org/dc/terms/\",\n" +
                "       \"type\": \"@type\",\n" +
                "        \"id\": \"@id\",\n" +
                "        \"isPartOf\": {\n" +
                "    \"@id\": \"dcterms:isPartOf\",\n" +
                "            \"@type\": \"@id\"\n" +
                "}}]\n" +
                "}";
    }

    /**
     * Post data to the provided URL
     *
     * @param url string containing URL
     * @param content string containing data
     * @param mimeType string containing MIMe type of content
     * @throws Exception in the event of an HTTP client failure
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
     *
     * @param fusekiBase string containing base uri
     * @param subject string containing subject
     * @throws Exception in the event of an HTTP client failure
     */
    public static void populateFuseki(final String fusekiBase, final String subject) throws Exception {
        httpPost(fusekiBase + "/update",
                sparqlUpdate(subject),
                "application/x-www-form-urlencoded");
    }

    /**
     * get a count of the items in the triplestore, corresponding to a given subject.
     *
     * @param fusekiBase string containing fueski base uri
     * @param subject string containing subject
     * @return count of triplestore items for given subject
     * @throws Exception in the event of an error converting a String to an Integer
     */
    public static Callable<Integer> triplestoreCount(final String fusekiBase, final String subject)  throws Exception {
        final String query = "SELECT (COUNT(*) AS ?n) WHERE { <" + subject + "> ?o ?p . }";
        final String url = fusekiBase + "/query?query=" + URLEncoder.encode(query, "UTF-8") + "&output=json";
        final ObjectMapper mapper = new ObjectMapper();
        return new Callable<Integer>() {
            public Integer call() throws Exception {
               final var results =  mapper.readTree(httpGet(url));
               final var count = Integer.valueOf(results.get("results")
                       .get("bindings").get(0).get("n").get("value").asText(), 10);
               return count;
            }
        };
    }

    /**
     * Create a new FcrepoClient instance with authentication.
     * @return the newly created client
     */
    public static FcrepoClient createClient() {
        return client().throwExceptionOnFailure()
                .authScope("localhost")
                .credentials(FEDORA_USERNAME, FEDORA_PASSWORD).build();
    }

    private TestUtils() {
        // prevent instantiation
    }
}
