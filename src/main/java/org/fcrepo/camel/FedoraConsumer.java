package org.fcrepo.camel;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.Message;
import org.apache.camel.impl.ScheduledPollConsumer;

/**
 * The Fedora consumer.
 */
public class FedoraConsumer extends ScheduledPollConsumer {
    private final FedoraEndpoint endpoint;
    private String path;
    private String type;

    public FedoraConsumer(FedoraEndpoint endpoint, Processor processor, String path, String type) {
        super(endpoint, processor);
        this.endpoint = endpoint;
        this.type = type;
        this.path = path;
    }

    @Override
    protected int poll() throws Exception {
        Exchange exchange = endpoint.createExchange();

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

        try {
            // send message to next processor in the route
            getProcessor().process(exchange);
            return 1; // number of messages polled
        } finally {
            // log exception if an exception occurred and was not handled
            if (exchange.getException() != null) {
                getExceptionHandler().handleException("Error processing exchange", exchange, exchange.getException());
            }
        }
    }
}
