package org.fcrepo.camel;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultProducer;
import org.apache.camel.util.ExchangeHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Fedora producer.
 */
public class FedoraProducer extends DefaultProducer {
    
    private static final Logger logger  = LoggerFactory.getLogger(FedoraProducer.class);

    private volatile FedoraEndpoint endpoint;
    private volatile String type;
    private volatile String path;

    public FedoraProducer(final FedoraEndpoint endpoint) {
        super(endpoint);
        this.endpoint = endpoint;
    }

    public void process(final Exchange exchange) throws Exception {
        final Message in = exchange.getIn();
        final FedoraClient client = new FedoraClient(
                endpoint.getAuthUsername(),
                endpoint.getAuthPassword(),
                endpoint.getAuthHost());

        String url = "http://" + endpoint.getBaseUrl();

        String contentType;
        String contentTypeString = ExchangeHelper.getContentType(exchange);
        if (endpoint.getType() != null) {
            contentType = endpoint.getType();
        } else if (contentTypeString != null) {
            contentType = contentTypeString;
        } else {
            contentType = endpoint.DEFAULT_CONTENT_TYPE;
        }

        if (in.getHeader(endpoint.FCREPO_IDENTIFIER) != null) {
            url += in.getHeader(endpoint.FCREPO_IDENTIFIER, String.class);
        } else if (in.getHeader(endpoint.FCREPO_JMS_IDENTIFIER) != null) {
            url += in.getHeader(endpoint.FCREPO_JMS_IDENTIFIER, String.class);
        }

        if (in.getBody() == null || in.getBody(String.class).isEmpty()) { 
            exchange.getIn().setBody(client.get(url, contentType));
        } else {
            exchange.getIn().setBody(client.post(url, in.getBody(String.class), contentType));
        }
        exchange.getIn().setHeader("Content-Type", contentType);
        client.stop();
    }
}
