package org.fcrepo.camel;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.Header;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

public class FedoraClient {

    private CloseableHttpClient httpclient;

    public FedoraClient() {
        this.httpclient = HttpClients.createDefault();
    }

    public void stop() throws ClientProtocolException, IOException {
        this.httpclient.close();
    }

    public String get(String url, final String type) throws ClientProtocolException, IOException {
        HttpGet httpget = new HttpGet(url);
        httpget.setHeader("Accept", type);

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
                        return entity != null ? EntityUtils.toString(entity) : null;
                    } else {
                        FedoraClient client = new FedoraClient();
                        String res = client.get(externalMetadata, type);
                        client.stop();
                        return res;
                    }
                } else {
                    throw new ClientProtocolException("Unexpected response status: " + status);
                }
            }
        };
        return httpclient.execute(httpget, responseHandler);
    }
}
