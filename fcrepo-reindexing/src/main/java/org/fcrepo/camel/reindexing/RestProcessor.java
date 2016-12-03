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
package org.fcrepo.camel.reindexing;

import static java.lang.String.join;
import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.Exchange.HTTP_RESPONSE_CODE;
import static org.fcrepo.camel.reindexing.ReindexingHeaders.REINDEXING_RECIPIENTS;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.slf4j.Logger;

/**
 * A processor that converts the REST uri into the
 * identifying path for an fcrepo node.
 *
 * This assumes that the `rest.prefix` value is stored
 * in the CamelFcrepoRestPrefix header.
 *
 * @author Aaron Coburn
 */
public class RestProcessor implements Processor {

    private static final Logger LOGGER = getLogger(RestProcessor.class);

    private static final int BAD_REQUEST = 400;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Convert the incoming REST request into the correct
     * Fcrepo header fields.
     *
     * @param exchange the current message exchange
     */
    public void process(final Exchange exchange) throws Exception {
        final Message in = exchange.getIn();

        final String contentType = in.getHeader(CONTENT_TYPE, "", String.class);
        final String body = in.getBody(String.class);
        final Set<String> endpoints = new HashSet<>();

        for (final String s : in.getHeader(REINDEXING_RECIPIENTS, "", String.class).split(",")) {
            endpoints.add(s.trim());
        }

        if (contentType.equals("application/json") && body != null && !body.trim().isEmpty()) {
            try {
                final JsonNode root = MAPPER.readTree(body);
                final Iterator<JsonNode> ite = root.elements();
                while (ite.hasNext()) {
                    final JsonNode n = ite.next();
                    endpoints.add(n.asText());
                }
            } catch (JsonProcessingException e) {
                LOGGER.debug("Invalid JSON", e);
                in.setHeader(HTTP_RESPONSE_CODE, BAD_REQUEST);
                in.setBody("Invalid JSON");
            }
        }
        in.setHeader(REINDEXING_RECIPIENTS, join(",", endpoints));
    }
}
