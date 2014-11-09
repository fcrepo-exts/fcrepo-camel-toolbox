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
import static org.fcrepo.camel.FedoraEndpoint.FCREPO_BASE_URL;
import static org.fcrepo.camel.FedoraEndpoint.FCREPO_IDENTIFIER;
import static org.fcrepo.jms.headers.DefaultMessageFactory.BASE_URL_HEADER_NAME;
import static org.fcrepo.jms.headers.DefaultMessageFactory.IDENTIFIER_HEADER_NAME;

import org.apache.camel.Processor;
import org.apache.camel.Exchange;
import org.apache.camel.Message;


/**
 * Represents a Processor class that formulates a Sparql DESCRIBE query
 * that is ready to be POSTed to a Sparql endpoint.
 *
 * The processor expects the following headers:
 *      org.fcrepo.jms.identifier
 *      org.fcrepo.jms.baseURL
 * each of which can be overridden with the following:
 *      FCREPO_IDENTIFIER
 *      FCREPO_BASE_URL
 *
 * @author Aaron Coburn
 * @since November 6, 2014
 */
public class SparqlDescribeProcessor implements Processor {
    /**
     *  Define how this message should be processed
     */
    public void process(final Exchange exchange) throws Exception {

        final Message in = exchange.getIn();

        String subject = null;

        if (in.getHeader(FCREPO_BASE_URL) != null) {
            subject = in.getHeader(FCREPO_BASE_URL, String.class);
        } else if (in.getHeader(BASE_URL_HEADER_NAME) != null) {
            subject = in.getHeader(BASE_URL_HEADER_NAME, String.class);
        } else {
            throw new Exception("No baseURL header available!");
        }

        if (in.getHeader(FCREPO_IDENTIFIER) != null) {
           subject += in.getHeader(FCREPO_IDENTIFIER);
        } else if (in.getHeader(IDENTIFIER_HEADER_NAME) != null) {
           subject += in.getHeader(IDENTIFIER_HEADER_NAME);
        }

        exchange.getIn().setBody("query=DESCRIBE <" + subject + ">");
        exchange.getIn().setHeader(HTTP_METHOD, "POST");
        exchange.getIn().setHeader(CONTENT_TYPE, "application/x-www-form-urlencoded");
    }
}
