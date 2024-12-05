/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package ch.docuteam.fcrepo.camel.processor;

import static org.apache.jena.util.URIref.encode;
import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

/**
 * This class provide a deleteWhere util method that is tied to the
 * docuteam datamodel compare to the standard one provided in ProcessorUtils
 *
 * @author Vincent Decorges
 */
public class DocuteamProcessorUtils {

    private static final Logger LOGGER  = getLogger(DocuteamProcessorUtils.class);

    private DocuteamProcessorUtils() {

    }

    /**
     * Create a DELETE WHERE { ... } statement from the provided subject
     * This is a custom implementation tied to the docuteam model.
     *
     * @param subject the subject of the triples to delete.
     * @param namedGraph an optional named graph
     * @return the delete statement
     */
    public static String deleteWhere(final String subject, final String namedGraph) {
        final StringBuilder stmt = new StringBuilder(
                "PREFIX rico: <https://www.ica.org/standards/RiC/ontology#>\nPREFIX premis: <http://www.loc.gov/premis/rdf/v3/>\nPREFIX schema: <http://schema.org/>\nDELETE { ?s ?p ?o } WHERE { ");

        if (!namedGraph.isEmpty()) {
            stmt.append("GRAPH ");
            stmt.append("<").append(encode(namedGraph)).append(">");
            stmt.append(" { ");
        }

        stmt.append(
                "?s rico:isOrWasIdentifierOf/rico:thingIsTargetOfRuleRelation/" +
                        "rico:ruleRelationHasSource/rico:regulatesOrRegulated ");
        stmt.append("<").append(encode(subject)).append("> . ");
        stmt.append("?s ?p ?o . ");

        if (!namedGraph.isEmpty()) {
            stmt.append("} ");
        }

        stmt.append("}");

        stmt.append(";\n");

        stmt.append("DELETE { ?s ?p ?o } WHERE { ");

        if (!namedGraph.isEmpty()) {
            stmt.append("GRAPH ");
            stmt.append("<").append(encode(namedGraph)).append(">");
            stmt.append(" { ");
        }

        stmt.append(
                "?s (rico:thingIsTargetOfEventRelation|rico:thingIsTargetOfRuleRelation)/" +
                        "(rico:ruleRelationHasSource|rico:eventRelationHasSource)" +
                        "/(rico:regulatesOrRegulated|rico:isEventAssociatedWith) ");
        stmt.append("<").append(encode(subject)).append("> . ");
        stmt.append("?s ?p ?o . ");

        if (!namedGraph.isEmpty()) {
            stmt.append("} ");
        }

        stmt.append("}");

        stmt.append(";\n");

        stmt.append("DELETE { ?s ?p ?o } WHERE { ");

        if (!namedGraph.isEmpty()) {
            stmt.append("GRAPH ");
            stmt.append("<").append(encode(namedGraph)).append(">");
            stmt.append(" { ");
        }

        stmt.append(
                "?s (rico:isOrWasIdentifierOf|rico:ruleRelationHasSource|" +
                        "rico:appellationIsSourceOfAppellationRelation|" +
                        "rico:placeIsSourceOfPlaceRelation|rico:agentIsTargetOfAgentOriginationRelation|" +
                        "rico:isDateAssociatedWith|rico:isBeginningDateOf|rico:isEndDateOf|" +
                        "rico:isOrWasAppellationOf|" +
                        "rico:regulatesOrRegulated|rico:isOrWasSubeventOf|rico:eventRelationHasSource|" +
                        "rico:ruleIsSourceOfRuleRelation|rico:eventIsSourceOfEventRelation)/" +
                        "(rico:regulatesOrRegulated|rico:appellationRelationHasTarget|rico:placeRelationHasTarget|" +
                        "rico:agentOriginationRelationHasSource|rico:isEventAssociatedWith|" +
                        "rico:ruleRelationHasTarget|" +
                        "rico:eventRelationHasTarget) ");
        stmt.append("<").append(encode(subject)).append("> . ");
        stmt.append("?s ?p ?o . ");

        if (!namedGraph.isEmpty()) {
            stmt.append("} ");
        }

        stmt.append("}");

        stmt.append(";\n");

        stmt.append("DELETE { ?s ?p ?o } WHERE { ");

        if (!namedGraph.isEmpty()) {
            stmt.append("GRAPH ");
            stmt.append("<").append(encode(namedGraph)).append(">");
            stmt.append(" { ");
        }

        stmt.append(
                "?s (rico:isOrWasTitleOf|rico:isOrWasIdentifierOf|rico:regulatesOrRegulated|" +
                        "rico:isDocumentaryFormTypeOf|" +
                        "rico:isOrWasAppellationOf|rico:isExtentOf|rico:isCarrierTypeOf|rico:isContentTypeOf|" +
                        "rico:isBeginningDateOf|rico:isEndDateOf|rico:isDateAssociatedWith|rico:isLastUpdateDateOf|" +
                        "rico:isRuleAssociatedWith|rico:isOrWasLanguageOf|rico:appellationRelationHasTarget|" +
                        "rico:placeRelationHasTarget|rico:agentOriginationRelationHasSource|" +
                        "rico:isEventAssociatedWith|" +
                        "rico:ruleRelationHasTarget|rico:eventRelationHasTarget|schema:position) ");
        stmt.append("<").append(encode(subject)).append("> . ");
        stmt.append("?s ?p ?o . ");

        if (!namedGraph.isEmpty()) {
            stmt.append("} ");
        }

        stmt.append("}");

        stmt.append(";\n");

        stmt.append("DELETE { ?s ?p ?o } WHERE { ");

        if (!namedGraph.isEmpty()) {
            stmt.append("GRAPH ");
            stmt.append("<").append(encode(namedGraph)).append(">");
            stmt.append(" { ");
        }

        stmt.append("<").append(encode(subject)).append("> ");
        stmt.append("premis:fixity ?s . ");
        stmt.append("?s ?p ?o . ");

        if (!namedGraph.isEmpty()) {
            stmt.append("} ");
        }

        stmt.append("}");

        stmt.append(";\n");

        stmt.append("DELETE WHERE { ");

        if (!namedGraph.isEmpty()) {
            stmt.append("GRAPH ");
            stmt.append("<").append(encode(namedGraph)).append(">");
            stmt.append(" { ");
        }

        stmt.append("<").append(encode(subject)).append("> ");
        stmt.append("?p ?o ");

        if (!namedGraph.isEmpty()) {
            stmt.append("} ");
        }

        stmt.append("}");

        return stmt.toString();
    }

}
