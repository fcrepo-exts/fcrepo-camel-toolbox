package org.fcrepo.camel;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultProducer;
import org.apache.camel.util.ExchangeHelper;
import org.apache.camel.component.http4.HttpMethods;

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
        final String contentTypeString = ExchangeHelper.getContentType(exchange);
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

        HttpMethods method = exchange.getIn().getHeader(Exchange.HTTP_METHOD, HttpMethods.class);
        if (method == null) {
            method = HttpMethods.GET;
        }

        switch (method) {
            case PATCH:
                exchange.getIn().setBody(client.patch(url, in.getBody(String.class)));
                break;
            case PUT:
                exchange.getIn().setBody(client.put(url, in.getBody(String.class), contentType));
                break;
            case POST:
                exchange.getIn().setBody(client.post(url, in.getBody(String.class), contentType));
                break;
            case DELETE:
                exchange.getIn().setBody(client.delete(url));
                break;
            case HEAD:
                client.head(url);
                exchange.getIn().setBody(null);
                break;
            case GET:
            default:
                if(endpoint.getMetadata()) {
                    exchange.getIn().setHeader("Content-Type", contentType);
                    exchange.getIn().setBody(client.get(url, contentType));
                } else {
                    exchange.getIn().setBody(client.get(url, null));
                }
        }
        client.stop();
    }
}
