/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.camel.common.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.lang3.StringUtils;

import static org.fcrepo.camel.common.helpers.BasicAuth.BASIC_AUTH_HEADER;
import static org.fcrepo.camel.common.helpers.BasicAuth.generateBasicAuthHeader;

/**
 * A processor for adding a basic auth header when username is present.
 * @author dbernstein
 */
public class AddBasicAuthProcessor implements Processor {

    private final String username;
    private final String password;

    /**
     * Constructor
     * @param username The username
     * @param password The password
     */
    public AddBasicAuthProcessor(final String username, final String password) {
        this.username = username;
        this.password = password;
    }
    @Override
    public void process(final Exchange exchange) throws Exception {
       if (!StringUtils.isBlank(this.username)) {
            exchange.getIn().setHeader(BASIC_AUTH_HEADER,
                    generateBasicAuthHeader(this.username, this.password));
       }
    }
}
