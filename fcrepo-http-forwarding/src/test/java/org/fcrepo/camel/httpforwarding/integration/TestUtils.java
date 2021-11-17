/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel.httpforwarding.integration;

import org.apache.camel.Exchange;
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
