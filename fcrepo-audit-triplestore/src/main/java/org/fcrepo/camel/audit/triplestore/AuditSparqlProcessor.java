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
package org.fcrepo.camel.audit.triplestore;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.apache.jena.riot.RDFDataMgr.write;
import static org.apache.jena.riot.RDFFormat.NTRIPLES;
import static org.apache.jena.vocabulary.RDF.type;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_AGENT;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_DATE_TIME;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_EVENT_ID;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_EVENT_TYPE;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_RESOURCE_TYPE;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.apache.jena.datatypes.xsd.XSDDatatype.XSDdateTime;
import static org.apache.jena.datatypes.xsd.XSDDatatype.XSDstring;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.apache.jena.rdf.model.ResourceFactory.createTypedLiteral;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.fcrepo.camel.processor.ProcessorUtils;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

/**
 * A processor that converts an audit message into a sparql-update
 * statement for an external triplestore.
 *
 * @author Aaron Coburn
 * @author escowles
 * @since 2015-04-09
 */

public class AuditSparqlProcessor implements Processor {

    static final String AUDIT = "http://fedora.info/definitions/v4/audit#";
    static final String PREMIS = "http://www.loc.gov/premis/rdf/v1#";
    static final String PROV = "http://www.w3.org/ns/prov#";
    static final String XSD = "http://www.w3.org/2001/XMLSchema#";
    static final String EVENT_TYPE = "http://id.loc.gov/vocabulary/preservation/eventType/";
    static final String EVENT_NAMESPACE = "http://fedora.info/definitions/v4/event#";
    static final String AS_NAMESPACE = "https://www.w3.org/ns/activitystreams#";
    static final String REPOSITORY = "http://fedora.info/definitions/v4/repository#";

    static final String CONTENT_MOD = AUDIT + "contentModification";
    static final String CONTENT_REM = AUDIT + "contentRemoval";
    static final String METADATA_MOD = AUDIT + "metadataModification";

    static final String CONTENT_ADD = EVENT_TYPE + "ing";
    static final String OBJECT_ADD = EVENT_TYPE + "cre";
    static final String OBJECT_REM = EVENT_TYPE + "del";

    static final String HAS_CONTENT = REPOSITORY + "hasContent";

    static final String NODE_ADDED = REPOSITORY + "NODE_ADDED";
    static final String NODE_REMOVED = REPOSITORY + "NODE_REMOVED";
    static final String PROPERTY_CHANGED = REPOSITORY + "PROPERTY_CHANGED";

    /**
     * Define how a message should be processed.
     *
     * @param exchange the current camel message exchange
     */
    public void process(final Exchange exchange) throws Exception {
        final Message in = exchange.getIn();
        final String eventURIBase = in.getHeader(AuditHeaders.EVENT_BASE_URI, String.class);
        final String eventID = in.getHeader(FCREPO_EVENT_ID, String.class);
        final Resource eventURI = createResource(eventURIBase + "/" + eventID);

        // generate SPARQL Update
        final StringBuilder query = new StringBuilder("update=");
        query.append(ProcessorUtils.insertData(serializedGraphForMessage(in, eventURI), ""));

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

    private static final String EMPTY_STRING = "";
    private static final String RESOURCE_TYPES = "org.fcrepo.jms.resourceType";

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
        @SuppressWarnings("unchecked")
        final List<String> eventType = message.getHeader(FCREPO_EVENT_TYPE, emptyList(), List.class);
        final String dateTime = message.getHeader(FCREPO_DATE_TIME, EMPTY_STRING, String.class);
        @SuppressWarnings("unchecked")
        final List<String> agents = message.getHeader(FCREPO_AGENT, emptyList(), List.class);
        @SuppressWarnings("unchecked")
        final List<String> resourceTypes = message.getHeader(FCREPO_RESOURCE_TYPE, emptyList(), List.class);
        final String identifier = message.getHeader(FCREPO_URI, EMPTY_STRING, String.class);
        final Optional<String> premisType = getAuditEventType(eventType, resourceTypes);

        model.add( model.createStatement(subject, type, INTERNAL_EVENT) );
        model.add( model.createStatement(subject, type, PREMIS_EVENT) );
        model.add( model.createStatement(subject, type, PROV_EVENT) );

        // basic event info
        model.add( model.createStatement(subject, PREMIS_TIME, createTypedLiteral(dateTime, XSDdateTime)) );
        model.add( model.createStatement(subject, PREMIS_OBJ, createResource(identifier)) );

        agents.forEach(agent -> {
            model.add( model.createStatement(subject, PREMIS_AGENT, createTypedLiteral(agent, XSDstring)) );
        });

        premisType.ifPresent(rdfType -> {
            model.add(model.createStatement(subject, PREMIS_TYPE, createResource(rdfType)));
        });

        write(serializedGraph, model, NTRIPLES);
        return serializedGraph.toString("UTF-8");
    }

    /**
     * Returns the Audit event type based on fedora event type and properties.
     *
     * @param eventType from Fedora
     * @param properties associated with the Fedora event
     * @return Audit event
     */
    private static Optional<String> getAuditEventType(final List<String> eventType, final List<String> resourceType) {
        // mapping event type/properties to audit event type
        if (eventType.contains(EVENT_NAMESPACE + "ResourceCreation") || eventType.contains(AS_NAMESPACE + "Create")) {
            if (resourceType.contains(REPOSITORY + "Binary")) {
                return of(CONTENT_ADD);
            } else {
                return of(OBJECT_ADD);
            }
        } else if (eventType.contains(EVENT_NAMESPACE + "ResourceDeletion") ||
                eventType.contains(AS_NAMESPACE + "Delete")) {
            if (resourceType.contains(REPOSITORY + "Binary")) {
                return of(CONTENT_REM);
            } else {
                return of(OBJECT_REM);
            }
        } else if (eventType.contains(EVENT_NAMESPACE + "ResourceModification") ||
                eventType.contains(AS_NAMESPACE + "Update")) {
            if (resourceType.contains(REPOSITORY + "Binary")) {
                return of(CONTENT_MOD);
            } else {
                return of(METADATA_MOD);
            }
        }
        return empty();
    }


}
