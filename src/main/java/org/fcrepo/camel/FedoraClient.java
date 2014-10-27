package org.fcrepo.camel;

import java.util.Map;
import java.util.HashMap;

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
import org.apache.http.client.ResponseHandler;
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

import java.io.IOException;

public class FedoraClient {

    private static final Logger logger = LoggerFactory.getLogger(FedoraClient.class);

    private CloseableHttpClient httpclient;

    public FedoraClient(final String username, final String password, final String host)  {
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

    protected static Map<String, String> extractResponseHeaders(Header[] responseHeaders) {
        if (responseHeaders == null || responseHeaders.length == 0) {
            return null;
        }

        Map<String, String> answer = new HashMap<String, String>();
        for (Header header : responseHeaders) {
            answer.put(header.getName(), header.getValue());
        }

        return answer;
    }
    

    public FedoraResponse head(final String url)
            throws ClientProtocolException, IOException, HttpOperationFailedException {

        HttpResponse response = httpclient.execute(new HttpHead(url));
        int status = response.getStatusLine().getStatusCode();
        Header locationHeader = response.getFirstHeader("location");
        HttpEntity entity = response.getEntity();
        
        if (status >= 200 && status < 300) {
            Header[] headers = response.getAllHeaders();
            String externalMetadata = "";
            for(Header header: headers) {
                if (header.getName().equals("Link")) {
                    String[] vals = header.getValue().split(";\\s*");
                    if (vals.length == 2 && vals[1].contains("describedby")) {
                        externalMetadata = vals[0].replaceAll("[<>]", "");
                    }
                }
            }
            if (externalMetadata.isEmpty()) {
                return new FedoraResponse(url, status, null, null);
            } else {
                return new FedoraResponse(url, status,
                        externalMetadata, null);
            }
        } else if (locationHeader != null && (status >= 300 && status < 400)) {
            
            throw new HttpOperationFailedException(url, status,
                    response.getStatusLine().getReasonPhrase(),
                    locationHeader.getValue(),
                    extractResponseHeaders(response.getAllHeaders()),
                    "");
        } else {
            throw new HttpOperationFailedException(url, status,
                    response.getStatusLine().getReasonPhrase(),
                    null,
                    extractResponseHeaders(response.getAllHeaders()),
                    "");
        }

    }

    public FedoraResponse put(final String url, final String body, final String contentType)
            throws ClientProtocolException, IOException, HttpOperationFailedException {

        HttpPut request = new HttpPut(url);
        request.addHeader("Content-Type", contentType);
        request.setEntity(new StringEntity(body));
        HttpResponse response = httpclient.execute(request);
        int status = response.getStatusLine().getStatusCode();
        Header locationHeader = response.getFirstHeader("location");
        HttpEntity entity = response.getEntity();
        
        if (status >= 200 && status < 300) {
            return new FedoraResponse(url, status, null, entity != null ? EntityUtils.toString(entity) : null);
        } else if (locationHeader != null && (status >= 300 && status < 400)) {
            
            throw new HttpOperationFailedException(url, status,
                    response.getStatusLine().getReasonPhrase(),
                    locationHeader.getValue(),
                    extractResponseHeaders(response.getAllHeaders()),
                    "");
        } else {
            throw new HttpOperationFailedException(url, status,
                    response.getStatusLine().getReasonPhrase(),
                    null,
                    extractResponseHeaders(response.getAllHeaders()),
                    "");
        }
    }

    public FedoraResponse patch(final String url, final String body) 
            throws ClientProtocolException, IOException, HttpOperationFailedException {
        FedoraResponse headResponse = this.head(url);
        
        HttpPatch request;
        if (headResponse.getLocation() != null) {
            request = new HttpPatch(headResponse.getLocation());
        } else {
            request = new HttpPatch(url);
        }
        request.addHeader("Content-Type", "application/sparql-update");
        request.setEntity(new StringEntity(body));
        HttpResponse response = httpclient.execute(request);
        int status = response.getStatusLine().getStatusCode();
        Header locationHeader = response.getFirstHeader("location");
        HttpEntity entity = response.getEntity();
        if (status >= 200 && status < 300) {
            return new FedoraResponse(url, status, null, entity != null ? EntityUtils.toString(entity) : null);
        } else if (locationHeader != null && (status >= 300 && status < 400)) {
            
            throw new HttpOperationFailedException(url, status,
                    response.getStatusLine().getReasonPhrase(),
                    locationHeader.getValue(),
                    extractResponseHeaders(response.getAllHeaders()),
                    "");
        } else {
            throw new HttpOperationFailedException(url, status,
                    response.getStatusLine().getReasonPhrase(),
                    null,
                    extractResponseHeaders(response.getAllHeaders()),
                    "");
        }
    }
        

    public FedoraResponse post(final String url, final String body, final String contentType)
            throws ClientProtocolException, IOException, HttpOperationFailedException {
       
        HttpPost request = new HttpPost(url);
        request.addHeader("Content-Type", contentType);
        request.setEntity(new StringEntity(body));

        HttpResponse response = httpclient.execute(request);
        int status = response.getStatusLine().getStatusCode();
        Header locationHeader = response.getFirstHeader("location");
        HttpEntity entity = response.getEntity();
        if (status >= 200 && status < 300) {
            return new FedoraResponse(url, status, null, entity != null ? EntityUtils.toString(entity) : null);
        } else if (locationHeader != null && (status >= 300 && status < 400)) {
            
            throw new HttpOperationFailedException(url, status,
                    response.getStatusLine().getReasonPhrase(),
                    locationHeader.getValue(),
                    extractResponseHeaders(response.getAllHeaders()),
                    "");
        } else {
            throw new HttpOperationFailedException(url, status,
                    response.getStatusLine().getReasonPhrase(),
                    null,
                    extractResponseHeaders(response.getAllHeaders()),
                    "");
        }
    }

    public FedoraResponse delete(final String url)
            throws ClientProtocolException, IOException, HttpOperationFailedException {

        HttpDelete request = new HttpDelete(url);
        HttpResponse response = httpclient.execute(request);
        int status = response.getStatusLine().getStatusCode();
        Header locationHeader = response.getFirstHeader("location");
        HttpEntity entity = response.getEntity();
        if (status >= 200 && status < 300) {
            return new FedoraResponse(url, status, null, entity != null ? EntityUtils.toString(entity) : null);
        } else if (locationHeader != null && (status >= 300 && status < 400)) {
            
            throw new HttpOperationFailedException(url, status,
                    response.getStatusLine().getReasonPhrase(),
                    locationHeader.getValue(),
                    extractResponseHeaders(response.getAllHeaders()),
                    "");
        } else {
            throw new HttpOperationFailedException(url, status,
                    response.getStatusLine().getReasonPhrase(),
                    null,
                    extractResponseHeaders(response.getAllHeaders()),
                    "");
        }
    }

    public FedoraResponse get(final String url, final String contentType)
            throws ClientProtocolException, IOException, HttpOperationFailedException {

        HttpGet request;
        
        if (contentType != null) {
            FedoraResponse headResponse = this.head(url);
            if (headResponse.getLocation() != null) {
                request = new HttpGet(headResponse.getLocation());
            } else {
                request = new HttpGet(url);
            }
            request.setHeader("Accept", contentType);
        } else {
            request = new HttpGet(url);
        }
        HttpResponse response = httpclient.execute(request);
        int status = response.getStatusLine().getStatusCode();
        Header locationHeader = response.getFirstHeader("location");
        HttpEntity entity = response.getEntity();
        if (status >= 200 && status < 300) {
            return new FedoraResponse(url, status, null, entity != null ? EntityUtils.toString(entity) : null);
        } else if (locationHeader != null && (status >= 300 && status < 400)) {
            throw new HttpOperationFailedException(url, status,
                    response.getStatusLine().getReasonPhrase(),
                    locationHeader.getValue(),
                    extractResponseHeaders(response.getAllHeaders()),
                    "");
        } else {
            throw new HttpOperationFailedException(url, status,
                    response.getStatusLine().getReasonPhrase(),
                    null,
                    extractResponseHeaders(response.getAllHeaders()),
                    "");
        }
    }
}
