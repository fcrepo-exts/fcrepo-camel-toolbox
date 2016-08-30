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
package org.fcrepo.camel.indexing.solr;

import static org.apache.commons.lang3.StringUtils.isBlank;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.fcrepo.camel.FcrepoHeaders;
import org.fcrepo.camel.JmsHeaders;

/**
 * A processor that converts an fcrepo message into
 * a delete command for Solr.
 *
 * @author acoburn
 * @since 2015-04-17
 */
public class SolrDeleteProcessor implements Processor {

    /**
     *  Format a message so that a record can be deleted in Solr.
     *
     *  The output format should be:
     *
     *  {
     *    "delete" : {
     *      "id" : "/foo"
     *    },
     *    "commitWithin" : 500
     *  }
     *
     *  @param exchange The incoming message exchange.
     */
    public void process(final Exchange exchange) throws Exception {

        final Message in = exchange.getIn();
        final ObjectMapper mapper = new ObjectMapper();

        if (isBlank(in.getHeader(FcrepoHeaders.FCREPO_IDENTIFIER, String.class))) {
            in.setHeader(FcrepoHeaders.FCREPO_IDENTIFIER,
                    in.getHeader(JmsHeaders.IDENTIFIER, String.class));
        }
        if (isBlank(in.getHeader(FcrepoHeaders.FCREPO_BASE_URL, String.class))) {
            in.setHeader(FcrepoHeaders.FCREPO_BASE_URL,
                    in.getHeader(JmsHeaders.BASE_URL, String.class));
        }


        final ObjectNode root = mapper.createObjectNode();

        root.putObject("delete")
                    .put("id",
                            in.getHeader(FcrepoHeaders.FCREPO_BASE_URL, String.class) +
                            in.getHeader(FcrepoHeaders.FCREPO_IDENTIFIER, String.class));

        in.setBody(mapper.writeValueAsString(root));
        in.setHeader(Exchange.CONTENT_TYPE, "application/json");
        in.setHeader(Exchange.HTTP_METHOD, "POST");
    }
}
