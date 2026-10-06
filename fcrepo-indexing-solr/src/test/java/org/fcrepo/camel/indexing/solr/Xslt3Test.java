/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel.indexing.solr;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.jupiter.api.Test;

/**
 * Verifies that the xslt-saxon component executes XSLT 3.0 stylesheets, and that
 * the XSLT 1.0 stylesheet shipped with the indexer still behaves as before.
 *
 * @author Dan Field
 */
public class Xslt3Test {

    private static final String RDF =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">" +
            "  <rdf:Description rdf:about=\"http://localhost:8080/fcrepo/rest/foo\">" +
            "    <rdf:type rdf:resource=\"http://www.w3.org/ns/ldp#RDFSource\"/>" +
            "    <rdf:type rdf:resource=\"http://fedora.info/definitions/v4/repository#Container\"/>" +
            "  </rdf:Description>" +
            "</rdf:RDF>";

    private String transform(final String endpointUri) throws Exception {
        try (var ctx = new DefaultCamelContext()) {
            ctx.addRoutes(new RouteBuilder() {
                @Override
                public void configure() {
                    from("direct:in").to(endpointUri);
                }
            });
            ctx.start();
            return ctx.createProducerTemplate().requestBody("direct:in", RDF, String.class);
        }
    }

    @Test
    public void testXslt3Features() throws Exception {
        final String out = transform("xslt-saxon:xslt3_transform.xsl");

        assertTrue(out.contains("<field name=\"rdftype_short\">ldp#RDFSource repository#Container</field>"),
                "higher-order function output missing: " + out);
        assertTrue(out.contains("\"ver\":\"3.0\"") || out.contains("\"ver\": \"3.0\""),
                "map/json serialization missing: " + out);
        assertTrue(out.contains("<field name=\"counter\">6</field>"),
                "xsl:iterate output missing: " + out);
    }

    @Test
    public void testXslt1StillWorks() throws Exception {
        final String out = transform("xslt-saxon:org/fcrepo/camel/indexing/solr/default_transform.xsl");

        assertTrue(out.contains("<field name=\"id\">http://localhost:8080/fcrepo/rest/foo</field>"), out);
        assertTrue(out.contains("<field name=\"rdftype\">http://www.w3.org/ns/ldp#RDFSource</field>"), out);
        assertTrue(out.contains("<field name=\"rdftype\">http://fedora.info/definitions/v4/repository#Container</field>"),
                out);
    }
}
