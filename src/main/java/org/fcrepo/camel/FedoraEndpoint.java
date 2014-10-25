package org.fcrepo.camel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.impl.DefaultEndpoint;

/**
 * Represents a Fedora endpoint.
 */
public class FedoraEndpoint extends DefaultEndpoint {

    public static final String HEADER_PREFIX = "org.fcrepo.jms.";

    public static final String HEADER_BASE_URL = HEADER_PREFIX + "baseURL";

    public static final String HEADER_IDENTIFIER = HEADER_PREFIX + "identifier";

    public static final String HEADER_EVENT_TYPE = HEADER_PREFIX + "eventType";

    public static final String HEADER_TIMESTAMP = HEADER_PREFIX + "timestamp";

    public static final String HEADER_PROPERTIES = HEADER_PREFIX + "properties";

    private volatile String type = "application/rdf+xml";

    private volatile String fullPath = "";

    private static final Logger logger = LoggerFactory.getLogger(FedoraEndpoint.class);

    public FedoraEndpoint(final String uri, final String remaining, final FedoraComponent component) {
        super(uri, component);
        this.setFullPath(remaining);
    }

    public FedoraEndpoint(final String endpointUri) {
        super(endpointUri);
    }

    public Producer createProducer() throws Exception {
        return new FedoraProducer(this, fullPath, type);
    }

    public Consumer createConsumer(final Processor processor) throws Exception {
        throw new RuntimeCamelException("Cannot produce to a FedoraEndpoint: " + getEndpointUri());
    }

    public boolean isSingleton() {
        return true;
    }

    public void setFullPath(final String fullPath) {
        this.fullPath = fullPath;
    }

    public String getFullPath() {
        return fullPath;
    }

    public void setType(final String type) {
        this.type = type.replaceAll(" ","+");
    }

    public String getType() {
        return type;
    }
}
