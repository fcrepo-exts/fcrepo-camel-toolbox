/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel.reindexing;

/**
 * A class defining header values for the indexing routes
 *
 * @author acoburn
 * @since May 22, 2015
 */
public final class ReindexingHeaders {
    public static final String REINDEXING_PORT = "CamelReindexingPort";
    public static final String REINDEXING_PREFIX = "CamelReindexingPrefix";
    public static final String REINDEXING_RECIPIENTS = "CamelReindexingRecipients";
    public static final String REINDEXING_HOST = "CamelReindexingHost";

    private ReindexingHeaders() {
        // prevent instantiation
    }
}
