/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel.audit.triplestore;

/**
 * @author acoburn
 */
public final class AuditHeaders {

    public static final String EVENT_BASE_URI = "CamelAuditEventBaseUri";

    public static final String EVENT_URI = "CamelAuditEventUri";

    private AuditHeaders() {
        // prevent instantiation
    }
}
