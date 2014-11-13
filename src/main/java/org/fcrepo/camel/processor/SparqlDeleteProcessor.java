/**
 * Copyright 2014 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.camel.processor;

import static org.apache.camel.Exchange.HTTP_METHOD;
import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.fcrepo.camel.FedoraEndpoint.FCREPO_BASE_URL;
import static org.fcrepo.camel.FedoraEndpoint.FCREPO_IDENTIFIER;
import static org.fcrepo.jms.headers.DefaultMessageFactory.BASE_URL_HEADER_NAME;
import static org.fcrepo.jms.headers.DefaultMessageFactory.IDENTIFIER_HEADER_NAME;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;

import org.apache.camel.Processor;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.commons.lang3.StringUtils;

import com.hp.hpl.jena.graph.Node_URI;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import java.io.InputStream;
import java.io.IOException;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

/**
 * Represends a message processor that deletes objects from an
 * external triplestore.
 *
 * @author Aaron Coburn
 * @since Nov 8, 2014
 */
public class SparqlDeleteProcessor implements Processor {
    /**
     * Define how the message should be processed.
     */
    public void process(final Exchange exchange) throws IOException {

        final Message in = exchange.getIn();
        String subject = null;

        if (in.getHeader(FCREPO_BASE_URL) != null) {
            subject = in.getHeader(FCREPO_BASE_URL, String.class);
        } else if (in.getHeader(BASE_URL_HEADER_NAME) != null) {
            subject = in.getHeader(BASE_URL_HEADER_NAME, String.class);
        } else {
            throw new IOException("No baseURL header available!");
        }

        if (in.getHeader(FCREPO_IDENTIFIER) != null) {
           subject += in.getHeader(FCREPO_IDENTIFIER);
        } else if (in.getHeader(IDENTIFIER_HEADER_NAME) != null) {
           subject += in.getHeader(IDENTIFIER_HEADER_NAME);
        }

        final Model model = createDefaultModel().read(in.getBody(InputStream.class), null);
        final StmtIterator triples = model.listStatements();

        // build list of triples to delete
        final Set<String> uris = new HashSet<String>();
        while ( triples.hasNext() ) {
            final Triple triple = triples.next().asTriple();

            // add subject uri, if it is part of this object
            if ( triple.getSubject().isURI() ) {
                final String uri = ((Node_URI)triple.getSubject()).getURI();

                if (uriMatches(subject, uri) ) {
                    uris.add(uri);
                }
            }

            // add object uri, if it is part of this object
            if ( triple.getObject().isURI() ) {
                final String uri = ((Node_URI)triple.getObject()).getURI();
                if (uriMatches(subject, uri) ) {
                    uris.add(uri);
                }
            }
        }

        // build delete commands
        final List<String> commands = new ArrayList<String>();
        for (final String uri : uris) {
            commands.add("DELETE WHERE { <" + uri + "> ?p ?o }");
        }

        exchange.getIn().setBody(StringUtils.join(commands, ";\n"));
        exchange.getIn().setHeader(HTTP_METHOD, "POST");
        exchange.getIn().setHeader(CONTENT_TYPE, "application/sparql-update");
    }

    private static boolean uriMatches(final String resource, final String candidate) {
        // All triples that will match this logic are ones that:
        // - have a candidate subject or object that equals the target resource of removal, or
        // - have a candidate subject or object that is prefixed with the resource of removal
        //    (therefore catching all children).
        return resource.equals(candidate) || candidate.startsWith(resource + "/")
            || candidate.startsWith(resource + "#");
    }
}
