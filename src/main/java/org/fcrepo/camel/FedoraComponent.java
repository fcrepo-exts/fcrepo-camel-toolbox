package org.fcrepo.camel;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.impl.DefaultComponent;

/**
 * Represents the component that manages {@link FedoraEndpoint}.
 */
public class FedoraComponent extends DefaultComponent {

    private static final Logger logger  = LoggerFactory.getLogger(FedoraComponent.class);

    public FedoraComponent() {
    }

    public FedoraComponent(CamelContext context) {
        super(context);
    }

    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        Endpoint endpoint = new FedoraEndpoint(uri, remaining, this);
        setProperties(endpoint, parameters);
        logger.info("Created Fedora Endpoint [{}]", endpoint);
        return endpoint;
    }
}
