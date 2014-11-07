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
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Link;

import org.apache.camel.component.http4.HttpOperationFailedException;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
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

    private CloseableHttpClient httpclient;

    private volatile Boolean throwExceptionOnFailure = true;

    public FedoraClient(final String username, final String password, final String host,
            final Boolean throwExceptionOnFailure) {
        
        this.throwExceptionOnFailure = throwExceptionOnFailure;
        
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        AuthScope scope = null;
        if ((username == null || username.isEmpty()) ||
                (password == null || password.isEmpty())) {
            this.httpclient = HttpClients.createDefault();
        } else {
            if (host != null) {
                scope = new AuthScope(new HttpHost(host));
            } 
            credsProvider.setCredentials(
                    scope,
                    new UsernamePasswordCredentials(username, password));
            this.httpclient = HttpClients.custom()
                    .setDefaultCredentialsProvider(credsProvider)
                    .build();
        }
    }

    public void stop() throws ClientProtocolException, IOException {
        this.httpclient.close();
    }
   

    public FedoraResponse head(final URI url)
            throws ClientProtocolException, IOException, HttpOperationFailedException {

        HttpHead request = new HttpHead(url);
        HttpResponse response = httpclient.execute(request);
        int status = response.getStatusLine().getStatusCode();
        final String contentType = getContentTypeHeader(response);
        
        if ((status >= 200 && status < 300) || !this.throwExceptionOnFailure) {
            HttpEntity entity = response.getEntity();
            URI describedBy = null;
            ArrayList<URI> links = getLinkHeaders(response, "describedby");
            if (links.size() == 1) {
                describedBy = links.get(0);
            }
            return new FedoraResponse(url, status, contentType, describedBy, null);
        } else {
            throw buildHttpOperationFailedException(url, response);
        }
    }

    public FedoraResponse put(final URI url, final String body, final String contentType)
            throws ClientProtocolException, IOException, HttpOperationFailedException {

        HttpPut request = new HttpPut(url);
        if (contentType != null) {
            request.addHeader("Content-Type", contentType);
        }
        if (body != null) {
            request.setEntity(new StringEntity(body));
        }

        HttpResponse response = httpclient.execute(request);
        int status = response.getStatusLine().getStatusCode();
        final String contentTypeHeader = getContentTypeHeader(response);
        
        if ((status >= 200 && status < 300) || !this.throwExceptionOnFailure) {
            HttpEntity entity = response.getEntity();
            return new FedoraResponse(url, status, contentTypeHeader, null, entity != null ? EntityUtils.toString(entity) : null);
        } else {
            throw buildHttpOperationFailedException(url, response);
        }
    }

    public FedoraResponse patch(final URI url, final String body) 
            throws ClientProtocolException, IOException, HttpOperationFailedException {
        
        HttpPatch request = new HttpPatch(url);
        request.addHeader("Content-Type", "application/sparql-update");
        if (body != null) {
            request.setEntity(new StringEntity(body));
        }

        HttpResponse response = httpclient.execute(request);
        int status = response.getStatusLine().getStatusCode();
        final String contentType = getContentTypeHeader(response);

        if ((status >= 200 && status < 300) || !this.throwExceptionOnFailure) {
            HttpEntity entity = response.getEntity();
            return new FedoraResponse(url, status, contentType, null, entity != null ? EntityUtils.toString(entity) : null);
        } else {
            throw buildHttpOperationFailedException(url, response);
        }
    }        

    public FedoraResponse post(final URI url, final String body, final String contentType)
            throws ClientProtocolException, IOException, HttpOperationFailedException {
       
        HttpPost request = new HttpPost(url);
        request.addHeader("Content-Type", contentType);
        if (body != null) {
            request.setEntity(new StringEntity(body));
        }

        HttpResponse response = httpclient.execute(request);
        int status = response.getStatusLine().getStatusCode();
        final String contentTypeHeader = getContentTypeHeader(response);

        if ((status >= 200 && status < 300) || !this.throwExceptionOnFailure) {
            HttpEntity entity = response.getEntity();
            return new FedoraResponse(url, status, contentTypeHeader, null, entity != null ? EntityUtils.toString(entity) : null);
        } else {
            throw buildHttpOperationFailedException(url, response);
        }
    }

    public FedoraResponse delete(final URI url)
            throws ClientProtocolException, IOException, HttpOperationFailedException {

        HttpDelete request = new HttpDelete(url);
        HttpResponse response = httpclient.execute(request);
        int status = response.getStatusLine().getStatusCode();
        final String contentType = getContentTypeHeader(response);
        
        if ((status >= 200 && status < 300) || !this.throwExceptionOnFailure) {
            HttpEntity entity = response.getEntity();
            return new FedoraResponse(url, status, contentType, null, entity != null ? EntityUtils.toString(entity) : null);
        } else {
            throw buildHttpOperationFailedException(url, response);
        }
    }

    public FedoraResponse get(final URI url, final String accept)
            throws ClientProtocolException, IOException, HttpOperationFailedException {

        HttpGet request = new HttpGet(url);
        
        if (accept != null) {
            request.setHeader("Accept", accept);
        }

        HttpResponse response = httpclient.execute(request);
        int status = response.getStatusLine().getStatusCode();
        final String contentType = getContentTypeHeader(response);

        if ((status >= 200 && status < 300) || !this.throwExceptionOnFailure) {
            HttpEntity entity = response.getEntity();
            URI describedBy = null;
            ArrayList<URI> links = getLinkHeaders(response, "describedby");
            if (links.size() == 1) {
                describedBy = links.get(0);
            }
            return new FedoraResponse(url, status, contentType, describedBy, entity != null ? EntityUtils.toString(entity) : null);
        } else {
            throw buildHttpOperationFailedException(url, response);
        }
    }

    protected static HttpOperationFailedException buildHttpOperationFailedException(final URI url, final HttpResponse response)
            throws IOException  {
        int status = response.getStatusLine().getStatusCode();
        Header locationHeader = response.getFirstHeader("location");
        HttpEntity entity = response.getEntity();
        String locationValue = null;
        
        if (locationHeader != null && (status >= 300 && status < 400)) {
            locationValue = locationHeader.getValue();
        }

        return new HttpOperationFailedException(url.toString(), status,
                response.getStatusLine().getReasonPhrase(),
                locationValue,
                extractResponseHeaders(response.getAllHeaders()),
                entity != null ? EntityUtils.toString(entity) : null);
                
    }
    
    protected static Map<String, String> extractResponseHeaders(final Header[] responseHeaders) {
        if (responseHeaders == null || responseHeaders.length == 0) {
            return null;
        }

        Map<String, String> answer = new HashMap<String, String>();
        for (Header header : responseHeaders) {
            answer.put(header.getName(), header.getValue());
        }

        return answer;
    }

    protected static String getContentTypeHeader(final HttpResponse response) {
        final Header[] contentTypes = response.getHeaders("Content-Type");
        if (contentTypes != null && contentTypes.length > 0) {
            return contentTypes[0].getValue();
        } else {
            return null;
        }
    }

    protected static ArrayList<URI> getLinkHeaders(final HttpResponse response, final String relationship) {
        ArrayList<URI> uris = new ArrayList<URI>();
        final Header[] links = response.getHeaders("Link");
        if (links != null) {
            for (Header header: links) {
                final Link link = Link.valueOf(header.getValue());
                if (link.getRel().equals(relationship)) {
                    uris.add(link.getUri());
                }
            }
        }
        return uris;
    }
 }
