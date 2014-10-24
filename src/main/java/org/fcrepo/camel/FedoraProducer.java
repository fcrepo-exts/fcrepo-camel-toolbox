package org.fcrepo.camel;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Fedora producer.
 */
public class FedoraProducer extends DefaultProducer {
    private static final Logger LOG = LoggerFactory.getLogger(FedoraProducer.class);
    private FedoraEndpoint endpoint;
    private String type;
    private String path;

    public FedoraProducer(FedoraEndpoint endpoint, String path, String type) {
        super(endpoint);
        this.endpoint = endpoint;
        this.type = type;
        this.path = path;
    }

    public void process(Exchange exchange) throws Exception {
        Message in = exchange.getIn();
        String url = "http://" + this.path;
        if(in.getHeader(endpoint.HEADER_BASE_URL) != null &&
                in.getHeader(endpoint.HEADER_IDENTIFIER) != null) {
            url = in.getHeader(endpoint.HEADER_BASE_URL, String.class)
                + in.getHeader(endpoint.HEADER_IDENTIFIER, String.class);
        }

        CloseableHttpClient httpclient = HttpClients.createDefault();         
        System.out.println(url);
        HttpGet httpget = new HttpGet(url);

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
        exchange.getIn().setBody(httpclient.execute(httpget, responseHandler));
        httpclient.close();
    }
}
