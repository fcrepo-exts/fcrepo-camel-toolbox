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

    public static final String JMS_HEADER_PREFIX = "org.fcrepo.jms.";

    public static final String FCREPO_JMS_IDENTIFIER =
        JMS_HEADER_PREFIX + "identifier";

    public static final String FCREPO_IDENTIFIER = "FCREPO_IDENTIFIER";

    public static final String DEFAULT_CONTENT_TYPE = "application/rdf+xml";

    private volatile String type = null;

    private volatile String baseUrl = "";

    private volatile String authUsername = null;

    private volatile String authPassword = null;

    private volatile String authHost = null;

    private volatile Boolean metadata = true;

    private static final Logger logger = LoggerFactory.getLogger(FedoraEndpoint.class);

    public FedoraEndpoint(final String uri, final String remaining, final FedoraComponent component) {
        super(uri, component);
        this.setBaseUrl(remaining);
    }

    public FedoraEndpoint(final String endpointUri) {
        super(endpointUri);
    }

    public Producer createProducer() throws Exception {
        return new FedoraProducer(this);
    }

    public Consumer createConsumer(final Processor processor) throws Exception {
        throw new RuntimeCamelException("Cannot produce to a FedoraEndpoint: " + getEndpointUri());
    }

    public boolean isSingleton() {
        return true;
    }

    public void setBaseUrl(final String url) {
        this.baseUrl = url;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setType(final String type) {
        this.type = type.replaceAll(" ", "+");
    }

    public String getType() {
        return type;
    }

    public void setAuthUsername(final String username) {
        this.authUsername = username;
    }

    public String getAuthUsername() {
        return authUsername;
    }

    public void setAuthPassword(final String password) {
        this.authPassword = password;
    }

    public String getAuthPassword() {
        return authPassword;
    }

    public String getAuthHost() {
        return authHost;
    }

    public void setAuthHost(final String host) {
        this.authHost = host;
    }

    public Boolean getMetadata() {
        return metadata;
    }

    public void setMetadata(final String metadata) {
        this.metadata = Boolean.valueOf(metadata);
    }
}
