/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package ch.docuteam.fcrepo.camel.processor;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;

/**
 * Aggregate Sparql queries
 *
 * @author Vincent Decorges
 */
public class SparqlAggregationStrategy implements AggregationStrategy {

    @Override
    public Exchange aggregate(final Exchange oldExchange, final Exchange newExchange) {
        if (oldExchange == null) {
            return newExchange; // First message in batch
        }

        final String oldBody = oldExchange.getIn().getBody(String.class);
        final String newBody = newExchange.getIn().getBody(String.class).replace("update=", "");

        final String aggregatedQuery = oldBody + ";\n" + newBody;

        oldExchange.getIn().setBody(aggregatedQuery);
        return oldExchange;
    }

}
