package org.fcrepo.camel;

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

        FedoraClient client = new FedoraClient();
        exchange.getIn().setBody(client.get(url, this.type));
        client.stop();
    }
}
