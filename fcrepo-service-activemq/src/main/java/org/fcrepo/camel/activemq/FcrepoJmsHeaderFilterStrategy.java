/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel.activemq;

import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.component.jms.JmsHeaderFilterStrategy;

/**
 * Retains the {@code Camel}-prefixed headers the reindexing workflow round-trips through the
 * broker queue. Camel's default JMS binding drops headers whose name starts with {@code Camel}
 * when a message is written to a JMS destination; the reindexing route relies on the target URI
 * and recipient list surviving the queue hop.
 *
 * @author Dan Field
 */
public class FcrepoJmsHeaderFilterStrategy extends JmsHeaderFilterStrategy {

    private static final Set<String> RETAINED_HEADERS = Set.of(
            "CamelFcrepoUri",
            "CamelReindexingRecipients");

    @Override
    public boolean applyFilterToCamelHeaders(final String headerName, final Object headerValue,
                                             final Exchange exchange) {
        if (RETAINED_HEADERS.contains(headerName)) {
            return false;
        }
        return super.applyFilterToCamelHeaders(headerName, headerValue, exchange);
    }

    @Override
    public boolean applyFilterToExternalHeaders(final String headerName, final Object headerValue,
                                                final Exchange exchange) {
        if (RETAINED_HEADERS.contains(headerName)) {
            return false;
        }
        return super.applyFilterToExternalHeaders(headerName, headerValue, exchange);
    }
}
