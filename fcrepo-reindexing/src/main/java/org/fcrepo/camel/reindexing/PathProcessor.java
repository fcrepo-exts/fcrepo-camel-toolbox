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

import java.net.URL;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.fcrepo.camel.FcrepoHeaders;

/**
 * A processor that converts the body of a message
 * (which should be a full fedora resource URI) into
 * the corresponding CamelFcrepoIdentifier value.
 *
 * @author Aaron Coburn
 */
public class PathProcessor implements Processor {

    /**
     *  Convert the message body into an appropriate IDENTIFIER value
     *
     *  @param exchange the current message exchange
     */
    public void process(final Exchange exchange) throws Exception {
        final Message in = exchange.getIn();

        final URL fullPath = new URL(in.getBody(String.class));
        final String base = in.getHeader(FcrepoHeaders.FCREPO_BASE_URL, String.class);
        final URL baseUrl = new URL(base.startsWith("http") ? base : "http://" + base);

        in.setHeader(FcrepoHeaders.FCREPO_IDENTIFIER, fullPath.getPath().substring(baseUrl.getPath().length()));

        in.removeHeader("JMSCorrelationID");
    }
}
