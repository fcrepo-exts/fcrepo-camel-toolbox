/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel.indexing.triplestore;

import static java.net.URLEncoder.encode;
import static java.util.Arrays.asList;
import static org.apache.camel.util.ObjectHelper.loadResourceAsStream;

import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ModelCamelContext;
import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

/**
 * Test the custom route workflow.
 *
 * @author Vincent Decorges
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {RouteTest.ContextConfig.class}, loader = AnnotationConfigContextLoader.class)
public class DocuteamRouteTest {

    private static final String REPOSITORY = "http://fedora.info/definitions/v4/repository#";

    @Produce("direct:start")
    protected ProducerTemplate template;

    @Autowired
    private CamelContext camelContext;

    @BeforeClass
    public static void beforeClass() {
        System.setProperty("triplestore.indexing.enabled", "true");
        System.setProperty("triplestore.indexing.predicate", "true");
        System.setProperty("triplestore.input.stream", "seda:foo");
        System.setProperty("triplestore.reindex.stream", "seda:reindex");
        System.setProperty("triplestore.using.docuteam.model", "true");
    }

    @DirtiesContext
    @Test
    public void testUpdateRouter() throws Exception {

        final String document = IOUtils.toString(loadResourceAsStream("container.nt"), "UTF-8").trim();
        final String responsePrefix =
                "PREFIX rico: <https://www.ica.org/standards/RiC/ontology#>\nPREFIX premis: " +
                        "<http://www.loc.gov/premis/rdf/v3/>\n" +
                        "PREFIX schema: <http://schema.org/>\n" +
                        "DELETE { ?s ?p ?o }" +
                        " WHERE { ?s rico:isOrWasIdentifierOf/rico:thingIsTargetOfRuleRelation/" +
                        "rico:ruleRelationHasSource/rico:regulatesOrRegulated <" +
                        RouteTest.baseURL + RouteTest.fileID +
                        "> . ?s ?p ?o . };\nDELETE { ?s ?p ?o } WHERE { ?s (rico:thingIsTargetOfEventRelation|" +
                        "rico:thingIsTargetOfRuleRelation)/(rico:ruleRelationHasSource|" +
                        "rico:eventRelationHasSource)/(rico:regulatesOrRegulated|" + "rico:isEventAssociatedWith) <" +
                        RouteTest.baseURL + RouteTest.fileID +
                        "> . ?s ?p ?o . };\nDELETE { ?s ?p ?o } WHERE { ?s (rico:isOrWasIdentifierOf|" +
                        "rico:ruleRelationHasSource|rico:appellationIsSourceOfAppellationRelation|" +
                        "rico:placeIsSourceOfPlaceRelation|rico:agentIsTargetOfAgentOriginationRelation|" +
                        "rico:isDateAssociatedWith|rico:isBeginningDateOf|rico:isEndDateOf|rico:isOrWasAppellationOf|" +
                        "rico:regulatesOrRegulated|rico:isOrWasSubeventOf|rico:eventRelationHasSource|" +
                        "rico:ruleIsSourceOfRuleRelation|rico:eventIsSourceOfEventRelation)/" +
                        "(rico:regulatesOrRegulated|rico:appellationRelationHasTarget|rico:placeRelationHasTarget|" +
                        "rico:agentOriginationRelationHasSource|rico:isEventAssociatedWith|" +
                        "rico:ruleRelationHasTarget|rico:eventRelationHasTarget) <" +
                        RouteTest.baseURL + RouteTest.fileID +
                        "> . ?s ?p ?o . };\nDELETE { ?s ?p ?o } WHERE { ?s " +
                        "(rico:isOrWasTitleOf|rico:isOrWasIdentifierOf|" +
                        "rico:regulatesOrRegulated|rico:isDocumentaryFormTypeOf|rico:isOrWasAppellationOf|" +
                        "rico:isExtentOf|rico:isCarrierTypeOf|rico:isContentTypeOf|rico:isBeginningDateOf|" +
                        "rico:isEndDateOf|rico:isDateAssociatedWith|rico:isLastUpdateDateOf|" +
                        "rico:isRuleAssociatedWith|" +
                        "rico:isOrWasLanguageOf|rico:appellationRelationHasTarget|rico:placeRelationHasTarget|" +
                        "rico:agentOriginationRelationHasSource|rico:isEventAssociatedWith|" +
                        "rico:ruleRelationHasTarget|rico:eventRelationHasTarget|schema:position) <" +
                        RouteTest.baseURL + RouteTest.fileID + "> . ?s ?p ?o . };\nDELETE { ?s ?p ?o } WHERE { <" +
                        RouteTest.baseURL + RouteTest.fileID +
                        "> premis:fixity ?s . ?s ?p ?o . };\nDELETE WHERE { <" + RouteTest.baseURL +
                        RouteTest.fileID + "> ?p ?o };\n" +
                        "INSERT DATA { ";

        final var context = camelContext.adapt(ModelCamelContext.class);

        AdviceWith.adviceWith(context, "FcrepoTriplestoreUpdater", a -> {
            a.mockEndpointsAndSkip("fcrepo*");
            a.mockEndpointsAndSkip("http*");
        });

        final MockEndpoint endpoint = MockEndpoint.resolve(camelContext, "mock:http:localhost:8080/fuseki/test/update");

        endpoint.expectedMessageCount(1);
        endpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");
        endpoint.expectedHeaderReceived(Exchange.CONTENT_TYPE, "application/x-www-form-urlencoded; charset=utf-8");
        endpoint.allMessages().body().startsWith("update=" + encode(responsePrefix, "UTF-8"));
        endpoint.allMessages().body().endsWith(encode("\n}", "UTF-8"));
        for (final String s : document.split("\n")) {
            endpoint.expectedBodyReceived().body().contains(encode(s, "UTF-8"));
        }

        final Map<String, Object> headers = RouteTest.createEvent(RouteTest.baseURL + RouteTest.fileID,
                asList(RouteTest.AS_NS + "Create"),
                asList(REPOSITORY + "Container"));
        headers.put(Exchange.CONTENT_TYPE, "application/rdf+xml");

        template.sendBodyAndHeaders("direct:update.triplestore",
                IOUtils.toString(loadResourceAsStream("container.rdf"), "UTF-8"),
                headers);

        endpoint.assertIsSatisfied();
    }
}
