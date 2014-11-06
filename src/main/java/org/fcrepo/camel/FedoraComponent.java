/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/license/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.camel;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.impl.DefaultComponent;

/**
 * Represents the component that manages {@link FedoraEndpoint}.
 * @author Aaron Coburn
 * @since October 20, 2014
 */
public class FedoraComponent extends DefaultComponent {

    private static final Logger LOGGER  = getLogger(FedoraComponent.class);

    /**
     * Create a FedoraComponent independent of a CamelContext.
     */
    public FedoraComponent() {
    }

    /**
     * Given a CamelContext, create a FedoraComponent instance.
     * @param context the CamelContext
     */
    public FedoraComponent(final CamelContext context) {
        super(context);
    }

    /**
     *  Create an Endpoint from a fcrepo uri along with an optional path value and attributes.
     *  @param uri the fcrepo uri identifying the repository hostname and port
     *  @param remaining the string identifying the repository path
     *  @param parameters any optional attributes added to the endpoint
     *  @throws Exception
     */
    @Override
    protected Endpoint createEndpoint(final String uri, final String remaining, final Map<String, Object> parameters) throws Exception {
        final Endpoint endpoint = new FedoraEndpoint(uri, remaining, this);
        setProperties(endpoint, parameters);
        LOGGER.info("Created Fedora Endpoint [{}]", endpoint);
        return endpoint;
    }
}
