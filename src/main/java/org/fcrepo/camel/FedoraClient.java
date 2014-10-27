package org.fcrepo.camel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public String head(final String url)
            throws ClientProtocolException, IOException {

        // Create a custom response handler
        ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
            public String handleResponse(
                    final HttpResponse response) throws ClientProtocolException, IOException {
                int status = response.getStatusLine().getStatusCode();
                if (status >= 200 && status < 300) {
                    HttpEntity entity = response.getEntity();
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
                        return url;
                    } else {
                        return externalMetadata;
                    }
                } else {
                    throw new ClientProtocolException("Unexpected response status: " + status);
                }
            }
        };
        return httpclient.execute(new HttpHead(url), responseHandler);
    }

    public String put(final String url, final String body, final String contentType)
            throws ClientProtocolException, IOException {
        ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
            public String handleResponse(
                    final HttpResponse response) throws ClientProtocolException, IOException {
                int status = response.getStatusLine().getStatusCode();
                if (status >= 200 && status < 300) {
                    HttpEntity entity = response.getEntity();
                    return entity != null ? EntityUtils.toString(entity) : null;
                } else {
                    throw new ClientProtocolException("Unexpected response status: " + status);
                }
            }
        };
        HttpPut request = new HttpPut(url);
        request.addHeader("Content-Type", contentType);
        request.setEntity(new StringEntity(body));
        return httpclient.execute(request, responseHandler);
    }

    public String patch(final String url, final String body) 
            throws ClientProtocolException, IOException {
        HttpPatch request = new HttpPatch(this.head(url));
        request.addHeader("Content-Type", "application/sparql-update");
        request.setEntity(new StringEntity(body));
        ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
            public String handleResponse(
                    final HttpResponse response) throws ClientProtocolException, IOException {
                int status = response.getStatusLine().getStatusCode();
                if (status >= 200 && status < 300) {
                    HttpEntity entity = response.getEntity();
                    return entity != null ? EntityUtils.toString(entity) : null;
                } else {
                    throw new ClientProtocolException("Unexpected response status: " + status);
                }
            }
        };
        return httpclient.execute(request, responseHandler);
    }
        

    public String post(final String url, final String body, final String contentType)
            throws ClientProtocolException, IOException {
       
        ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
            public String handleResponse(
                    final HttpResponse response) throws ClientProtocolException, IOException {
                int status = response.getStatusLine().getStatusCode();
                if (status >= 200 && status < 300) {
                    HttpEntity entity = response.getEntity();
                    return entity != null ? EntityUtils.toString(entity) : null;
                } else {
                    throw new ClientProtocolException("Unexpected response status: " + status);
                }
            }
        };

        HttpPost request = new HttpPost(url);
        request.addHeader("Content-Type", contentType);
        request.setEntity(new StringEntity(body));

        return httpclient.execute(request, responseHandler);
    }

    public String delete(final String url)
            throws ClientProtocolException, IOException {
        // Create a custom response handler
        ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
            public String handleResponse(
                    final HttpResponse response) throws ClientProtocolException, IOException {
                int status = response.getStatusLine().getStatusCode();
                if (status >= 200 && status < 300) {
                    HttpEntity entity = response.getEntity();
                    return entity != null ? EntityUtils.toString(entity) : null;
                } else {
                    throw new ClientProtocolException("Unexpected response status: " + status);
                }
            }
        };
        HttpDelete request = new HttpDelete(url);
        return httpclient.execute(request, responseHandler);
    }

    public String get(final String url, final String contentType) throws ClientProtocolException, IOException {

        HttpGet request;
        
        // Create a custom response handler
        ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
            public String handleResponse(
                    final HttpResponse response) throws ClientProtocolException, IOException {
                int status = response.getStatusLine().getStatusCode();
                if (status >= 200 && status < 300) {
                    HttpEntity entity = response.getEntity();
                    return entity != null ? EntityUtils.toString(entity) : null;
                } else {
                    throw new ClientProtocolException("Unexpected response status: " + status);
                }
            }
        };

        if (contentType != null) {
            request = new HttpGet(this.head(url));
            request.setHeader("Accept", contentType);
        } else {
            request = new HttpGet(url);
        }
        return httpclient.execute(request, responseHandler);
    }
}
