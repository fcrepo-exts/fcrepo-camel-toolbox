/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
        // TODO: use the OSGi service registry as described in:
        // http://blog.osgi.org/2013/02/javautilserviceloader-in-osgi.html
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
