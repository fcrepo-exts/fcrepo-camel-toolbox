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
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;

import org.apache.camel.Processor;
import org.apache.camel.Exchange;
import org.apache.camel.Message;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.modify.request.QuadDataAcc;
import com.hp.hpl.jena.sparql.modify.request.UpdateDataInsert;
import com.hp.hpl.jena.update.UpdateRequest;

import java.io.InputStream;
import java.io.IOException;

/**
 * Represents a processor for creating the sparql-update message to
 * be passed to an external triplestore.
 *
 * @author Aaron Coburn
 * @since Nov 8, 2014
 */
public class SparqlInsertProcessor implements Processor {
    /**
     * Define how the message is processed.
     */
    public void process(final Exchange exchange) throws IOException {

        final Message in = exchange.getIn();
        final Model model = createDefaultModel().read(in.getBody(InputStream.class), null, "N-TRIPLE");
        final StmtIterator triples = model.listStatements();
        final QuadDataAcc add = new QuadDataAcc();
        while (triples.hasNext()) {
            add.addTriple(triples.nextStatement().asTriple());
        }
        final UpdateRequest request = new UpdateRequest(new UpdateDataInsert(add));

        exchange.getIn().setBody(request.toString());
        exchange.getIn().setHeader(HTTP_METHOD, "POST");
        exchange.getIn().setHeader(CONTENT_TYPE, "application/sparql-update");
    }
}
