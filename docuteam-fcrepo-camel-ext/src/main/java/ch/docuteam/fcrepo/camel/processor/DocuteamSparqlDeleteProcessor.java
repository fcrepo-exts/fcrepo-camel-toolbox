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
import static org.fcrepo.camel.processor.ProcessorUtils.getSubjectUri;

import java.nio.charset.StandardCharsets;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;

/**
 * Represents a message processor that deletes objects from an
 * external triplestore.
 * This implementation used the docuteam model and was derived from the
 * standard implementation in fcrepo-camel.
 *
 * @author Vincent Decorges
 */
public class DocuteamSparqlDeleteProcessor implements Processor {

    @Override
    public void process(final Exchange exchange) throws Exception {
        final Message in = exchange.getIn();
        final String namedGraph = in.getHeader(FCREPO_NAMED_GRAPH, "", String.class);
        final String subject = getSubjectUri(exchange);

        in.setBody("update=" + encode(deleteWhere(subject, namedGraph), StandardCharsets.UTF_8));
        in.setHeader(HTTP_METHOD, "POST");
        in.setHeader(CONTENT_TYPE, "application/x-www-form-urlencoded; charset=utf-8");
    }
}
