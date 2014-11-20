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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Link;

import org.apache.camel.component.http4.HttpOperationFailedException;
import org.apache.http.HttpStatus;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

/**
 * Represents a client to interact with Fedora's HTTP API.
 *
 * Note: This should be swapped out to use https://github.com/fcrepo4-labs/fcrepo4-client
 *
 * @author Aaron Coburn
 * @since October 20, 2014
 */
public class FedoraClient {

    private static final String DESCRIBED_BY = "describedby";

    private static final String CONTENT_TYPE = "Content-Type";

    private CloseableHttpClient httpclient;

    private Boolean throwExceptionOnFailure = true;

    /**
     * Create a FedoraClient with a set of authentication values.
     * @param username the username for the repository
     * @param password the password for the repository
     * @param host the authentication hostname (realm) for the repository
     * @param throwExceptionOnFailure whether to throw an exception on any non-2xx or 3xx HTTP responses
     */
    public FedoraClient(final String username, final String password, final String host,
            final Boolean throwExceptionOnFailure) {

        final CredentialsProvider credsProvider = new BasicCredentialsProvider();
        AuthScope scope = null;

        this.throwExceptionOnFailure = throwExceptionOnFailure;

        if (username == null || username.isEmpty() ||
                password == null || password.isEmpty()) {
            this.httpclient = HttpClients.createDefault();
        } else {
            if (host != null) {
                scope = new AuthScope(new HttpHost(host));
            } else {
                scope = new AuthScope(AuthScope.ANY);
            }
            credsProvider.setCredentials(
                    scope,
                    new UsernamePasswordCredentials(username, password));
            this.httpclient = HttpClients.custom()
                    .setDefaultCredentialsProvider(credsProvider)
                    .build();
        }
    }

    /**
     * Stop the client
     */
    public void stop() throws IOException {
        this.httpclient.close();
    }

    /**
     * Make a HEAD response
     * @param url the URL of the resource to check
     */
    public FedoraResponse head(final URI url)
            throws IOException, HttpOperationFailedException {

        final HttpHead request = new HttpHead(url);
        final HttpResponse response = httpclient.execute(request);
        final int status = response.getStatusLine().getStatusCode();
        final String contentType = getContentTypeHeader(response);

        if ((status >= HttpStatus.SC_OK && status < HttpStatus.SC_BAD_REQUEST) || !this.throwExceptionOnFailure) {
            URI describedBy = null;
            final List<URI> links = getLinkHeaders(response, DESCRIBED_BY);
            if (links.size() == 1) {
                describedBy = links.get(0);
            }
            return new FedoraResponse(url, status, contentType, describedBy, null);
        } else {
            throw buildHttpOperationFailedException(url, response);
        }
    }

    /**
     * Make a PUT request
     * @param url the URL of the resource to PUT
     * @param body the contents of the resource to send
     * @param contentType the MIMEType of the resource
     */
    public FedoraResponse put(final URI url, final InputStream body, final String contentType)
            throws IOException, HttpOperationFailedException {

        final HttpPut request = new HttpPut(url);
        if (contentType != null) {
            request.addHeader(CONTENT_TYPE, contentType);
        }
        if (body != null) {
            request.setEntity(new InputStreamEntity(body));
        }

        final HttpResponse response = httpclient.execute(request);
        final int status = response.getStatusLine().getStatusCode();
        final String contentTypeHeader = getContentTypeHeader(response);

        if ((status >= HttpStatus.SC_OK && status < HttpStatus.SC_BAD_REQUEST) || !this.throwExceptionOnFailure) {
            final HttpEntity entity = response.getEntity();
            return new FedoraResponse(url, status, contentTypeHeader, null,
                    entity != null ? EntityUtils.toString(entity) : null);
        } else {
            throw buildHttpOperationFailedException(url, response);
        }
    }

    /**
     * Make a PATCH request
     * Please note: the body should have an application/sparql-update content-type
     * @param url the URL of the resource to PATCH
     * @param body the body to be sent to the repository
     */
    public FedoraResponse patch(final URI url, final InputStream body)
            throws IOException, HttpOperationFailedException {

        final HttpPatch request = new HttpPatch(url);
        request.addHeader(CONTENT_TYPE, "application/sparql-update");
        if (body != null) {
            request.setEntity(new InputStreamEntity(body));
        }

        final HttpResponse response = httpclient.execute(request);
        final int status = response.getStatusLine().getStatusCode();
        final String contentType = getContentTypeHeader(response);

        if ((status >= HttpStatus.SC_OK && status < HttpStatus.SC_BAD_REQUEST) || !this.throwExceptionOnFailure) {
            final HttpEntity entity = response.getEntity();
            return new FedoraResponse(url, status, contentType, null,
                    entity != null ? EntityUtils.toString(entity) : null);
        } else {
            throw buildHttpOperationFailedException(url, response);
        }
    }

    /**
     * Make a POST request
     * @param url the URL of the resource to which to POST
     * @param body the content to be sent to the server
     * @param contentType the Content-Type of the body
     */
    public FedoraResponse post(final URI url, final InputStream body, final String contentType)
            throws IOException, HttpOperationFailedException {

        final HttpPost request = new HttpPost(url);
        request.addHeader(CONTENT_TYPE, contentType);
        if (body != null) {
            request.setEntity(new InputStreamEntity(body));
        }

        final HttpResponse response = httpclient.execute(request);
        final int status = response.getStatusLine().getStatusCode();
        final String contentTypeHeader = getContentTypeHeader(response);

        if ((status >= HttpStatus.SC_OK && status < HttpStatus.SC_BAD_REQUEST) || !this.throwExceptionOnFailure) {
            final HttpEntity entity = response.getEntity();
            return new FedoraResponse(url, status, contentTypeHeader, null,
                    entity != null ? EntityUtils.toString(entity) : null);
        } else {
            throw buildHttpOperationFailedException(url, response);
        }
    }

    /**
     * Make a DELETE request
     * @param url the URL of the resource to delete
     */
    public FedoraResponse delete(final URI url)
            throws IOException, HttpOperationFailedException {

        final HttpDelete request = new HttpDelete(url);
        final HttpResponse response = httpclient.execute(request);
        final int status = response.getStatusLine().getStatusCode();
        final String contentType = getContentTypeHeader(response);

        if ((status >= HttpStatus.SC_OK && status < HttpStatus.SC_BAD_REQUEST) || !this.throwExceptionOnFailure) {
            final HttpEntity entity = response.getEntity();
            return new FedoraResponse(url, status, contentType, null,
                    entity != null ? EntityUtils.toString(entity) : null);
        } else {
            throw buildHttpOperationFailedException(url, response);
        }
    }

    /**
     * Make a GET request
     * @param url the URL of the resource to fetch
     * @param accept the requested MIMEType of the resource to be retrieved
     */
    public FedoraResponse get(final URI url, final String accept)
            throws IOException, HttpOperationFailedException {

        final HttpGet request = new HttpGet(url);

        if (accept != null) {
            request.setHeader("Accept", accept);
        }

        final HttpResponse response = httpclient.execute(request);
        final int status = response.getStatusLine().getStatusCode();
        final String contentType = getContentTypeHeader(response);

        if ((status >= HttpStatus.SC_OK && status < HttpStatus.SC_BAD_REQUEST) || !this.throwExceptionOnFailure) {
            final HttpEntity entity = response.getEntity();
            URI describedBy = null;
            final List<URI> links = getLinkHeaders(response, DESCRIBED_BY);
            if (links.size() == 1) {
                describedBy = links.get(0);
            }
            return new FedoraResponse(url, status, contentType, describedBy,
                    entity != null ? EntityUtils.toString(entity) : null);
        } else {
            throw buildHttpOperationFailedException(url, response);
        }
    }

    /**
     * Build a HttpOperationFailedException object from an http response
     * @param url the URL of the request
     * @param response the HTTP response
     */
    protected static HttpOperationFailedException
        buildHttpOperationFailedException(final URI url, final HttpResponse response)
            throws IOException  {

        final int status = response.getStatusLine().getStatusCode();
        final Header locationHeader = response.getFirstHeader("location");
        final HttpEntity entity = response.getEntity();
        String locationValue = null;

        if (locationHeader != null) {
            locationValue = locationHeader.getValue();
        }

        return new HttpOperationFailedException(url.toString(), status,
                response.getStatusLine().getReasonPhrase(),
                locationValue,
                extractResponseHeaders(response.getAllHeaders()),
                entity != null ? EntityUtils.toString(entity) : null);
    }

    /**
     * Extract the response headers into a Map
     */
    protected static Map<String, String> extractResponseHeaders(final Header[] responseHeaders) {
        if (responseHeaders == null) {
            return null;
        }

        final Map<String, String> answer = new HashMap<String, String>();
        for (Header header : responseHeaders) {
            answer.put(header.getName(), header.getValue());
        }

        return answer;
    }

    /**
     * Extract the content-type header value
     */
    protected static String getContentTypeHeader(final HttpResponse response) {
        final Header[] contentTypes = response.getHeaders(CONTENT_TYPE);
        if (contentTypes.length > 0) {
            return contentTypes[0].getValue();
        } else {
            return null;
        }
    }

    /**
     * Extract any Link headers
     */
    protected static List<URI> getLinkHeaders(final HttpResponse response, final String relationship) {
        final List<URI> uris = new ArrayList<URI>();
        final Header[] links = response.getHeaders("Link");
        for (Header header: links) {
            final Link link = Link.valueOf(header.getValue());
            if (link.getRel().equals(relationship)) {
                uris.add(link.getUri());
            }
        }
        return uris;
    }
 }
