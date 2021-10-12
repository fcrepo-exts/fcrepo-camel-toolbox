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

package org.fcrepo.camel.common.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.lang3.StringUtils;

import static org.apache.camel.builder.Builder.simple;
import static org.fcrepo.camel.common.helpers.BasicAuth.BASIC_AUTH_HEADER;
import static org.fcrepo.camel.common.helpers.BasicAuth.generateBasicAuthHeader;

/**
 * A processor for adding a basic auth header when username is present.
 * @author dbernstein
 */
public class AddBasicAuthProcessor implements Processor {

    private String username;
    private String password;

    /**
     * Constructor
     * @param username The username
     * @param password The password
     */
    public AddBasicAuthProcessor(final String username, final String password) {
        this.username = username;
        this.password = password;
    }
    @Override
    public void process(Exchange exchange) throws Exception {
       if(!StringUtils.isBlank(this.username)) {
            exchange.getIn().setHeader(BASIC_AUTH_HEADER,
                    generateBasicAuthHeader(this.username, this.password));
       }
    }
}
