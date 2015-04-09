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

import org.apache.camel.Processor;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.fcrepo.camel.JmsHeaders;

/**
 * A processor that converts an audit message into a sparql-update
 * statement for an external triplestore.
 *
 * @author Aaron Coburn
 */

public class AuditSparqlProcessor implements Processor {

    /**
     * Define how a message should be processed.
     *
     * @param exchange the current camel message exchange
     */
    public void process(final Exchange exchange) throws Exception {
        final Message in = exchange.getIn();

        // A UUID for this event -- this can be generated here or come from an existing header
        final String eventId = in.getHeader("CamelFcrepoAuditEventId", String.class);
        // this is a list that will need to be split on commas
        final String eventType = in.getHeader(JmsHeaders.EVENT_TYPE, String.class);
        final String baseUrl = in.getHeader(JmsHeaders.BASE_URL, String.class);
        final String identifier = in.getHeader(JmsHeaders.IDENTIFIER, String.class);
        final Long timestamp = Long.parseLong(in.getHeader(JmsHeaders.TIMESTAMP, String.class), 10);
        final String user = in.getHeader(JmsHeaders.USER, String.class);
        final String userAgent = in.getHeader(JmsHeaders.USER_AGENT, String.class);
        // this is a list that will need to be split on commas
        final String properties = in.getHeader(JmsHeaders.PROPERTIES, String.class);
        // Once fcrepo-camel has these headers, use const vals
        final String fixity = in.getHeader("org.fcrepo.jms.fixity", String.class);
        final String digest = in.getHeader("org.fcrepo.jms.contentDigest", String.class);
        final Integer size = in.getHeader("org.fcrepo.jms.contentSize", Integer.class);

        final StringBuilder query = new StringBuilder("update=");

        // add triples here (Jena or Clerezza, whichever is easiest)

        in.setBody(query.toString());
        in.setHeader(Exchange.CONTENT_TYPE, "application/x-www-form-urlencoded");
        in.setHeader(Exchange.HTTP_METHOD, "POST");
    }
}
