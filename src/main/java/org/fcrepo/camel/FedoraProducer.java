package org.fcrepo.camel;

import java.net.URI;

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

    public FedoraProducer(final FedoraEndpoint endpoint) {
        super(endpoint);
        this.endpoint = endpoint;
    }

    public void process(final Exchange exchange) throws Exception {
        final Message in = exchange.getIn();
        final FedoraClient client = new FedoraClient(
                endpoint.getAuthUsername(),
                endpoint.getAuthPassword(),
                endpoint.getAuthHost(),
                endpoint.getThrowExceptionOnFailure());

        final HttpMethods method = this.getMethod(exchange);
        final String contentType = this.getContentType(exchange);
        final String url = this.getUrl(exchange);
            
        logger.info("Fcrepo Request [{}] with method [{}]", url, method);

        FedoraResponse headResponse;
        FedoraResponse response;
        
        switch (method) {
            case PATCH:
                headResponse = client.head(new URI(url));
                if (headResponse.getLocation() != null) {
                    response = client.patch(headResponse.getLocation(), in.getBody(String.class));
                } else {
                    response = client.patch(new URI(url), in.getBody(String.class));
                }
                exchange.getIn().setBody(response.getBody());
                break;
            case PUT:
                response = client.put(new URI(url), in.getBody(String.class), contentType);
                exchange.getIn().setBody(response.getBody());
                break;
            case POST:
                response = client.post(new URI(url), in.getBody(String.class), contentType);
                exchange.getIn().setBody(response.getBody());
                break;
            case DELETE:
                response = client.delete(new URI(url));
                exchange.getIn().setBody(response.getBody());
                break;
            case HEAD:
                response = client.head(new URI(url));
                exchange.getIn().setBody(null);
                break;
            case GET:
            default:
                if(endpoint.getMetadata()) {
                    headResponse = client.head(new URI(url));
                    if (headResponse.getLocation() != null) {
                        response = client.get(headResponse.getLocation(), contentType);
                    } else {
                        response = client.get(new URI(url), contentType);
                    }
                } else {
                    response = client.get(new URI(url), null);
                }
                exchange.getIn().setBody(response.getBody());
                exchange.getIn().setHeader("Content-Type", response.getContentType());
        }
        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, response.getStatusCode());
        client.stop();
    }

    protected HttpMethods getMethod(final Exchange exchange) {
        HttpMethods method = exchange.getIn().getHeader(Exchange.HTTP_METHOD, HttpMethods.class);
        if (method == null) {
            method = HttpMethods.GET;
        }
        return method;
    }

    protected String getContentType(final Exchange exchange) {
        final String contentTypeString = ExchangeHelper.getContentType(exchange);
        String contentType;
        if (endpoint.getContentType() != null) {
            contentType = endpoint.getContentType();
        } else if (contentTypeString != null) {
            contentType = contentTypeString;
        } else {
            contentType = endpoint.DEFAULT_CONTENT_TYPE;
        }
        return contentType;
    }

    protected String getUrl(final Exchange exchange) {
        final Message in = exchange.getIn();
        final HttpMethods method = exchange.getIn().getHeader(Exchange.HTTP_METHOD, HttpMethods.class);
        String url = "http://" + endpoint.getBaseUrl();
        if (in.getHeader(endpoint.FCREPO_IDENTIFIER) != null) {
            url += in.getHeader(endpoint.FCREPO_IDENTIFIER, String.class);
        } else if (in.getHeader(endpoint.FCREPO_JMS_IDENTIFIER) != null) {
            url += in.getHeader(endpoint.FCREPO_JMS_IDENTIFIER, String.class);
        }
        if (endpoint.getTransform() != null) {
            if (method == HttpMethods.POST) {
                url += "/fcr:transform";
            } else if (method == null || method == HttpMethods.GET) {
                url += "/fcr:transform/" + endpoint.getTransform();
            }
        } else if (method == HttpMethods.DELETE && endpoint.getTombstone()) {
            url += "/fcr:tombstone";
        }
        return url;
    }
}
