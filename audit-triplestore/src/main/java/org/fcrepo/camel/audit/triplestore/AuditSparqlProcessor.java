/**
 * Copyright 2015 DuraSpace, Inc.
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

import static org.fcrepo.camel.RdfNamespaces.RDF;
import static org.fcrepo.camel.RdfNamespaces.REPOSITORY;
import static org.fcrepo.camel.audit.triplestore.AuditNamespaces.AUDIT;
import static org.fcrepo.camel.audit.triplestore.AuditNamespaces.EVENT_TYPE;
import static org.fcrepo.camel.audit.triplestore.AuditNamespaces.PREMIS;
import static org.fcrepo.camel.audit.triplestore.AuditNamespaces.PROV;
import static org.fcrepo.camel.audit.triplestore.AuditNamespaces.XSD;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import org.fcrepo.camel.JmsHeaders;
import org.fcrepo.camel.processor.ProcessorUtils;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.core.impl.TypedLiteralImpl;
import org.apache.clerezza.rdf.core.serializedform.SerializingProvider;
import org.apache.clerezza.rdf.jena.serializer.JenaSerializerProvider;


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
        final String UUIDString = UUID.randomUUID().toString();
        final UriRef eventURI = new UriRef(eventURIBase + "/" + UUIDString);
        final Set<Triple> triples = triplesForMessage(in, eventURI);

        // serialize triples
        final SerializingProvider serializer = new JenaSerializerProvider();
        final ByteArrayOutputStream serializedGraph = new ByteArrayOutputStream();
        serializer.serialize(serializedGraph, new SimpleMGraph(triples), "text/rdf+nt");

        // generate SPARQL Update
        final StringBuilder query = new StringBuilder("update=");
        query.append(ProcessorUtils.insertData(serializedGraph.toString("UTF-8"), null));

        // update exchange
        in.setBody(query.toString());
        in.setHeader(AuditHeaders.EVENT_URI, eventURI.toString());
        in.setHeader(Exchange.CONTENT_TYPE, "application/x-www-form-urlencoded");
        in.setHeader(Exchange.HTTP_METHOD, "POST");
    }

    // namespaces and properties
    private static final UriRef INTERNAL_EVENT = new UriRef(AUDIT + "InternalEvent");
    private static final UriRef PREMIS_EVENT = new UriRef(PREMIS + "Event");
    private static final UriRef PROV_EVENT = new UriRef(PROV + "InstantaneousEvent");

    private static final UriRef CONTENT_MOD = new UriRef(AUDIT + "contentModification");
    private static final UriRef CONTENT_REM = new UriRef(AUDIT + "contentRemoval");
    private static final UriRef METADATA_MOD = new UriRef(AUDIT + "metadataModification");

    private static final UriRef CONTENT_ADD = new UriRef(EVENT_TYPE + "ing");
    private static final UriRef OBJECT_ADD = new UriRef(EVENT_TYPE + "cre");
    private static final UriRef OBJECT_REM = new UriRef(EVENT_TYPE + "del");

    private static final UriRef PREMIS_TIME = new UriRef(PREMIS + "hasEventDateTime");
    private static final UriRef PREMIS_OBJ = new UriRef(PREMIS + "hasEventRelatedObject");
    private static final UriRef PREMIS_AGENT = new UriRef(PREMIS + "hasEventRelatedAgent");
    private static final UriRef PREMIS_TYPE = new UriRef(PREMIS + "hasEventType");

    private static final UriRef RDF_TYPE = new UriRef(RDF + "type");
    private static final UriRef XSD_DATE = new UriRef(XSD + "dateTime");
    private static final UriRef XSD_STRING = new UriRef(XSD + "string");

    private static final String HAS_CONTENT = REPOSITORY + "hasContent";
    private static final String LAST_MODIFIED = REPOSITORY + "lastModified";
    private static final String NODE_ADDED = REPOSITORY + "NODE_ADDED";
    private static final String NODE_REMOVED = REPOSITORY + "NODE_REMOVED";
    private static final String PROPERTY_CHANGED = REPOSITORY + "PROPERTY_CHANGED";

    private static final String EMPTY_STRING = "";

    /**
     * Convert a Camel message to audit event description.
     * @param message Camel message produced by an audit event
     * @param subject RDF subject of the audit description
     */
    private static Set<Triple> triplesForMessage(final Message message, final UriRef subject) throws IOException {

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

        // types
        final Set<Triple> triples = new HashSet<>();
        triples.add( new TripleImpl(subject, RDF_TYPE, INTERNAL_EVENT) );
        triples.add( new TripleImpl(subject, RDF_TYPE, PREMIS_EVENT) );
        triples.add( new TripleImpl(subject, RDF_TYPE, PROV_EVENT) );

        // basic event info
        triples.add( new TripleImpl(subject, PREMIS_TIME, new TypedLiteralImpl(date, XSD_DATE)) );
        triples.add( new TripleImpl(subject, PREMIS_OBJ, new UriRef(identifier)) );
        triples.add( new TripleImpl(subject, PREMIS_AGENT, new TypedLiteralImpl(user, XSD_STRING)) );
        triples.add( new TripleImpl(subject, PREMIS_AGENT, new TypedLiteralImpl(agent, XSD_STRING)) );

        // mapping event type/properties to audit event type
        if (eventType.contains(NODE_ADDED)) {
            if (properties != null && properties.contains(HAS_CONTENT)) {
                triples.add( new TripleImpl(subject, PREMIS_TYPE, CONTENT_ADD) );
            } else {
                triples.add( new TripleImpl(subject, PREMIS_TYPE, OBJECT_ADD) );
            }
        } else if (eventType.contains(NODE_REMOVED)) {
            if (properties != null && properties.contains(HAS_CONTENT)) {
                triples.add( new TripleImpl(subject, PREMIS_TYPE, CONTENT_REM) );
            } else {
                triples.add( new TripleImpl(subject, PREMIS_TYPE, OBJECT_REM) );
            }
        } else if (eventType.contains(PROPERTY_CHANGED)) {
            if (properties != null && properties.contains(HAS_CONTENT)) {
                triples.add( new TripleImpl(subject, PREMIS_TYPE, CONTENT_MOD) );
            } else if (properties != null && properties.equals(LAST_MODIFIED)) {
                /* adding/removing a file updates the lastModified property of the parent container,
                   so ignore updates when only lastModified is changed */
            } else {
                triples.add( new TripleImpl(subject, PREMIS_TYPE, METADATA_MOD) );
            }
        }
        return triples;
    }
}
