/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel.activemq;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Test;

/**
 * Tests the JMS header filter strategy that keeps the reindexing workflow headers across the queue hop.
 *
 * @author fcrepo-camel-toolbox
 */
public class FcrepoJmsHeaderFilterStrategyTest {

    private final FcrepoJmsHeaderFilterStrategy strategy = new FcrepoJmsHeaderFilterStrategy();

    @Test
    public void retainsReindexingHeadersOnOutput() {
        assertFalse(strategy.applyFilterToCamelHeaders("CamelFcrepoUri", "http://localhost/rest", null),
                "CamelFcrepoUri must be written to the JMS message");
        assertFalse(strategy.applyFilterToCamelHeaders("CamelReindexingRecipients", "mock:result", null),
                "CamelReindexingRecipients must be written to the JMS message");
    }

    @Test
    public void retainsReindexingHeadersOnInput() {
        assertFalse(strategy.applyFilterToExternalHeaders("CamelFcrepoUri", "http://localhost/rest", null),
                "CamelFcrepoUri must be read back from the JMS message");
        assertFalse(strategy.applyFilterToExternalHeaders("CamelReindexingRecipients", "mock:result", null),
                "CamelReindexingRecipients must be read back from the JMS message");
    }

    @Test
    public void stillFiltersOtherCamelHeadersOnOutput() {
        final Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        assertTrue(strategy.applyFilterToCamelHeaders("CamelSomethingElse", "value", exchange),
                "unrelated Camel-internal headers should still be filtered from the JMS message");
    }

    @Test
    public void allowsUnrelatedExternalHeadersOnInput() {
        final Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        assertFalse(strategy.applyFilterToExternalHeaders("someBrokerProperty", "value", exchange),
                "unrelated inbound headers should pass through to the exchange");
    }
}
