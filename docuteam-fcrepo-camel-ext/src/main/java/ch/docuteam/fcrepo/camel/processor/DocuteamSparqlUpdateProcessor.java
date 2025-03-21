/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package ch.docuteam.fcrepo.camel.processor;

import static ch.docuteam.fcrepo.camel.processor.DocuteamProcessorUtils.deleteWhere;

import static java.net.URLEncoder.encode;

import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.Exchange.HTTP_METHOD;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_NAMED_GRAPH;

import static org.apache.http.entity.ContentType.parse;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.riot.RDFDataMgr.read;
import static org.apache.jena.riot.RDFLanguages.contentTypeToLang;
import static org.fcrepo.camel.processor.ProcessorUtils.getSubjectUri;
import static org.fcrepo.camel.processor.ProcessorUtils.insertData;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.jena.rdf.model.Model;

/**
 * Represents a processor for creating the sparql-update message to
 * be passed to an external triplestore.
 * This implementation used the docuteam model and was derived from the
 * standard implementation in fcrepo-camel.
 *
 * @author Vincent Decorges
 */
public class DocuteamSparqlUpdateProcessor implements Processor {

    @Override
    public void process(final Exchange exchange) throws Exception {
        final Message in = exchange.getIn();

        final ByteArrayOutputStream serializedGraph = new ByteArrayOutputStream();
        final String namedGraph = in.getHeader(FCREPO_NAMED_GRAPH, "", String.class);
        final Model model = createDefaultModel();
        final String subject = getSubjectUri(exchange);

        read(model, in.getBody(InputStream.class),
                contentTypeToLang(parse(in.getHeader(CONTENT_TYPE, String.class)).getMimeType()));

        model.write(serializedGraph, "N-TRIPLE");

        in.setBody("update=" + encode(deleteWhere(subject, namedGraph) + ";\n" +
                insertData(serializedGraph.toString(StandardCharsets.UTF_8),
                        namedGraph), StandardCharsets.UTF_8));

        in.setHeader(HTTP_METHOD, "POST");
        in.setHeader(CONTENT_TYPE, "application/x-www-form-urlencoded; charset=utf-8");
    }
}
