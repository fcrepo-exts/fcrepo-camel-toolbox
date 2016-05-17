/*
 * Copyright 2016 DuraSpace, Inc.
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
package org.fcrepo.camel.audit.triplestore;

import static org.fcrepo.audit.AuditNamespaces.AUDIT;
import static org.fcrepo.audit.AuditNamespaces.PREMIS;
import static org.fcrepo.audit.AuditNamespaces.PROV;
import static org.fcrepo.camel.RdfNamespaces.RDF;
import static com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDdateTime;
import static com.hp.hpl.jena.datatypes.xsd.XSDDatatype.XSDstring;
import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createProperty;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createTypedLiteral;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.fcrepo.audit.AuditUtils;
import org.fcrepo.camel.JmsHeaders;
import org.fcrepo.camel.processor.ProcessorUtils;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * A processor that converts an audit message into a sparql-update
 * statement for an external triplestore.
 *
 * @author Aaron Coburn
 * @author escowles
 * @since 2015-04-09
 */

public class AuditSparqlProcessor implements Processor {

    /**
     * Define how a message should be processed.
     *
     * @param exchange the current camel message exchange
     */
    public void process(final Exchange exchange) throws Exception {
        final Message in = exchange.getIn();
        final String eventURIBase = in.getHeader(AuditHeaders.EVENT_BASE_URI, String.class);
        final String eventID = in.getHeader(JmsHeaders.EVENT_ID, String.class);
        final Resource eventURI = createResource(eventURIBase + "/" + eventID);

        // generate SPARQL Update
        final StringBuilder query = new StringBuilder("update=");
        query.append(ProcessorUtils.insertData(serializedGraphForMessage(in, eventURI), null));

        // update exchange
        in.setBody(query.toString());
        in.setHeader(AuditHeaders.EVENT_URI, eventURI.toString());
        in.setHeader(Exchange.CONTENT_TYPE, "application/x-www-form-urlencoded");
        in.setHeader(Exchange.HTTP_METHOD, "POST");
    }

    // namespaces and properties
    private static final Resource INTERNAL_EVENT = createResource(AUDIT + "InternalEvent");
    private static final Resource PREMIS_EVENT = createResource(PREMIS + "Event");
    private static final Resource PROV_EVENT = createResource(PROV + "InstantaneousEvent");

    private static final Property PREMIS_TIME = createProperty(PREMIS + "hasEventDateTime");
    private static final Property PREMIS_OBJ = createProperty(PREMIS + "hasEventRelatedObject");
    private static final Property PREMIS_AGENT = createProperty(PREMIS + "hasEventRelatedAgent");
    private static final Property PREMIS_TYPE = createProperty(PREMIS + "hasEventType");
    private static final Property RDF_TYPE = createProperty(RDF + "type");

    private static final String EMPTY_STRING = "";

    /**
     * Convert a Camel message to audit event description.
     * @param message Camel message produced by an audit event
     * @param subject RDF subject of the audit description
     */
    private static String serializedGraphForMessage(final Message message, final Resource subject) throws IOException {

        // serialize triples
        final ByteArrayOutputStream serializedGraph = new ByteArrayOutputStream();
        final Model model = createDefaultModel();

        // get info from jms message headers
        final String eventType = (String) message.getHeader(JmsHeaders.EVENT_TYPE, EMPTY_STRING);
        final Long timestamp =  (Long) message.getHeader(JmsHeaders.TIMESTAMP, 0);
        final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        final String date = df.format(new Date(timestamp));
        final String user = (String) message.getHeader(JmsHeaders.USER, EMPTY_STRING);
        final String agent = (String) message.getHeader(JmsHeaders.USER_AGENT, EMPTY_STRING);
        final String properties = (String) message.getHeader(JmsHeaders.PROPERTIES, EMPTY_STRING);
        final String identifier = ProcessorUtils.getSubjectUri(message);
        final String premisType = AuditUtils.getAuditEventType(eventType, properties);

        model.add( model.createStatement(subject, RDF_TYPE, INTERNAL_EVENT) );
        model.add( model.createStatement(subject, RDF_TYPE, PREMIS_EVENT) );
        model.add( model.createStatement(subject, RDF_TYPE, PROV_EVENT) );

        // basic event info
        model.add( model.createStatement(subject, PREMIS_TIME, createTypedLiteral(date, XSDdateTime)) );
        model.add( model.createStatement(subject, PREMIS_OBJ, createResource(identifier)) );
        model.add( model.createStatement(subject, PREMIS_AGENT, createTypedLiteral(user, XSDstring)) );
        model.add( model.createStatement(subject, PREMIS_AGENT, createTypedLiteral(agent, XSDstring)) );
        if (premisType != null) {
            model.add(model.createStatement(subject, PREMIS_TYPE, createResource(premisType)));
        }

        model.write(serializedGraph, "N-TRIPLE");
        return serializedGraph.toString("UTF-8");
    }
}
