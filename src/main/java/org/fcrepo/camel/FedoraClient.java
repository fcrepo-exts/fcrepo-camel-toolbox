package org.fcrepo.camel;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.camel.component.http4.HttpOperationFailedException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.entity.StringEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.util.EntityUtils;

import org.apache.commons.lang.StringUtils;

import java.io.IOException;

public class FedoraClient {

    private static final Logger logger = LoggerFactory.getLogger(FedoraClient.class);

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
   

    public FedoraResponse head(final String url)
            throws ClientProtocolException, IOException, HttpOperationFailedException {

        HttpHead request = new HttpHead(url);
        HttpResponse response = httpclient.execute(request);
        int status = response.getStatusLine().getStatusCode();
        
        if ((status >= 200 && status < 300) || !this.throwExceptionOnFailure) {
            HttpEntity entity = response.getEntity();
            String describedBy = null;
            ArrayList<String> links = getLinkHeaders(response, "describedby");
            if (links.size() == 1) {
                describedBy = links.get(0);
            }
            return new FedoraResponse(url, status, describedBy, null);
        } else {
            throw buildHttpOperationFailedException(url, response);
        }
    }

    public FedoraResponse put(final String url, final String body, final String contentType)
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
        
        if ((status >= 200 && status < 300) || !this.throwExceptionOnFailure) {
            HttpEntity entity = response.getEntity();
            return new FedoraResponse(url, status, null, entity != null ? EntityUtils.toString(entity) : null);
        } else {
            throw buildHttpOperationFailedException(url, response);
        }
    }

    public FedoraResponse patch(final String url, final String body) 
            throws ClientProtocolException, IOException, HttpOperationFailedException {
        
        HttpPatch request = new HttpPatch(url);
        request.addHeader("Content-Type", "application/sparql-update");
        if (body != null) {
            request.setEntity(new StringEntity(body));
        }

        HttpResponse response = httpclient.execute(request);
        int status = response.getStatusLine().getStatusCode();

        if ((status >= 200 && status < 300) || !this.throwExceptionOnFailure) {
            HttpEntity entity = response.getEntity();
            return new FedoraResponse(url, status, null, entity != null ? EntityUtils.toString(entity) : null);
        } else {
            throw buildHttpOperationFailedException(url, response);
        }
    }        

    public FedoraResponse post(final String url, final String body, final String contentType)
            throws ClientProtocolException, IOException, HttpOperationFailedException {
       
        HttpPost request = new HttpPost(url);
        request.addHeader("Content-Type", contentType);
        if (body != null) {
            request.setEntity(new StringEntity(body));
        }

        HttpResponse response = httpclient.execute(request);
        int status = response.getStatusLine().getStatusCode();

        if ((status >= 200 && status < 300) || !this.throwExceptionOnFailure) {
            HttpEntity entity = response.getEntity();
            return new FedoraResponse(url, status, null, entity != null ? EntityUtils.toString(entity) : null);
        } else {
            throw buildHttpOperationFailedException(url, response);
        }
    }

    public FedoraResponse delete(final String url)
            throws ClientProtocolException, IOException, HttpOperationFailedException {

        HttpDelete request = new HttpDelete(url);
        HttpResponse response = httpclient.execute(request);
        int status = response.getStatusLine().getStatusCode();
        
        if ((status >= 200 && status < 300) || !this.throwExceptionOnFailure) {
            HttpEntity entity = response.getEntity();
            return new FedoraResponse(url, status, null, entity != null ? EntityUtils.toString(entity) : null);
        } else {
            throw buildHttpOperationFailedException(url, response);
        }
    }

    public FedoraResponse get(final String url, final String contentType)
            throws ClientProtocolException, IOException, HttpOperationFailedException {

        HttpGet request = new HttpGet(url);
        
        if (contentType != null) {
            request.setHeader("Accept", contentType);
        }

        HttpResponse response = httpclient.execute(request);
        int status = response.getStatusLine().getStatusCode();

        if ((status >= 200 && status < 300) || !this.throwExceptionOnFailure) {
            HttpEntity entity = response.getEntity();
            String describedBy = null;
            ArrayList<String> links = getLinkHeaders(response, "describedby");
            if (links.size() == 1) {
                describedBy = links.get(0);
            }
            return new FedoraResponse(url, status, describedBy, entity != null ? EntityUtils.toString(entity) : null);
        } else {
            throw buildHttpOperationFailedException(url, response);
        }
    }

    protected static HttpOperationFailedException buildHttpOperationFailedException(final String url, final HttpResponse response)
            throws IOException  {
        int status = response.getStatusLine().getStatusCode();
        Header locationHeader = response.getFirstHeader("location");
        HttpEntity entity = response.getEntity();
        String locationValue = null;
        
        if (locationHeader != null && (status >= 300 && status < 400)) {
            locationValue = locationHeader.getValue();
        }

        return new HttpOperationFailedException(url, status,
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

    protected static ArrayList<String> getLinkHeaders(final HttpResponse response, final String relationship) {
        ArrayList<String> uris = new ArrayList<String>();
        for(Header header: response.getAllHeaders()) {
            if (header.getName().equals("Link")) {
                // Link: <http://localhost:8080/fcrepo/rest/path/to/resource>; rel="describedby"
                // Split on ; character
                String[] toks = StringUtils.split(header.getValue(), ';');
                if (toks.length == 2) {
                    // Split the rel="..." portion on '='
                    String[] rel = StringUtils.split(toks[1], '=');
                    // Strip off optional quotes and spaces, test if it equals the `relationship` string
                    if (StringUtils.strip(rel[1], "\" ").equals(relationship)) {
                        // Strip angle brackets and spaces from the URL
                        uris.add(StringUtils.stripEnd(StringUtils.stripStart(toks[0], "< "), "> "));
                    }
                }
            }
        }
        return uris;
    }
 }
