/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel.ldpath;

import static java.util.Collections.singletonList;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.jsonldjava.sesame.SesameJSONLDParserFactory;
import org.apache.marmotta.ldpath.LDPath;
import org.apache.marmotta.ldpath.api.functions.SelectorFunction;
import org.apache.marmotta.ldpath.backend.linkeddata.LDCacheBackend;
import org.apache.marmotta.ldpath.exception.LDPathParseException;
import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.resultio.BooleanQueryResultParserRegistry;
import org.openrdf.query.resultio.TupleQueryResultParserRegistry;
import org.openrdf.query.resultio.sparqlxml.SPARQLBooleanXMLParserFactory;
import org.openrdf.query.resultio.sparqlxml.SPARQLResultsXMLParserFactory;
import org.openrdf.rio.RDFParserRegistry;
import org.openrdf.rio.n3.N3ParserFactory;
import org.openrdf.rio.ntriples.NTriplesParserFactory;
import org.openrdf.rio.rdfjson.RDFJSONParserFactory;
import org.openrdf.rio.rdfxml.RDFXMLParserFactory;
import org.openrdf.rio.trig.TriGParserFactory;
import org.openrdf.rio.turtle.TurtleParserFactory;
import org.semarglproject.sesame.rdf.rdfa.SesameRDFaParserFactory;

/**
 * A convenience factory for creating an LDPath object with an LDCacheBackend.
 * @author acoburn
 * @since Aug 5, 2016
 */
public class LDPathWrapper {

    private final LDPath<Value> ldpath;

    /**
     * Create an LDPathWrapper and register a set of selector functions.
     *
     * @param backend the linkeddata backend
     * @param functions selector functions
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public LDPathWrapper(final LDCacheBackend backend, final Set<SelectorFunction> functions) {
        this(backend);
        for (SelectorFunction<Value> function : functions) {
            ldpath.registerFunction(function);
        }
    }

    /**
     * Create an LDPathWrapper Object
     * @param backend the linkeddata backend
     */
    public LDPathWrapper(final LDCacheBackend backend) {

        // Register the Sesame RDF Parsers manually
        RDFParserRegistry.getInstance().add(new RDFXMLParserFactory());
        RDFParserRegistry.getInstance().add(new NTriplesParserFactory());
        RDFParserRegistry.getInstance().add(new TurtleParserFactory());
        RDFParserRegistry.getInstance().add(new N3ParserFactory());
        RDFParserRegistry.getInstance().add(new SesameJSONLDParserFactory());
        RDFParserRegistry.getInstance().add(new RDFJSONParserFactory());
        RDFParserRegistry.getInstance().add(new SesameRDFaParserFactory());
        RDFParserRegistry.getInstance().add(new TriGParserFactory());
        BooleanQueryResultParserRegistry.getInstance().add(new SPARQLBooleanXMLParserFactory());
        TupleQueryResultParserRegistry.getInstance().add(new SPARQLResultsXMLParserFactory());

        ldpath = new LDPath<Value>(backend);
    }

    /**
     * Execute an LDPath query
     * @param uri the URI to query
     * @param program the LDPath program
     * @return a result object wrapped in a List
     * @throws LDPathParseException if the LDPath program was malformed
     */
    public List<Map<String, Collection<?>>> programQuery(final String uri, final InputStream program)
            throws LDPathParseException {
        return singletonList(ldpath.programQuery(new URIImpl(uri), new InputStreamReader(program)));
    }
}
