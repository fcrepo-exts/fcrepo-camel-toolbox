/*
 * Copyright 2016 DuraSpace, Inc.
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
package org.fcrepo.camel.reindexing;

import java.io.StringWriter;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.fcrepo.camel.FcrepoHeaders;

/**
 * A processor to generate some useful documentation on the usage
 * of this service.
 *
 * @author Aaron Coburn
 */
public class UsageProcessor implements Processor {

    /**
     * Convert the incoming REST request into some useful
     * documentation.
     *
     * @param exchange the current message exchange
     */
    public void process(final Exchange exchange) throws Exception {
        final Message in = exchange.getIn();
        final MustacheFactory mf = new DefaultMustacheFactory();
        final Map<String, Object> scopes = new HashMap<>();

        scopes.put("fedora", in.getHeader(FcrepoHeaders.FCREPO_BASE_URL, "", String.class));
        scopes.put("reindexing", InetAddress.getLocalHost().getHostName() + ":" +
                in.getHeader(ReindexingHeaders.REST_PORT, "", String.class) +
                in.getHeader(ReindexingHeaders.REST_PREFIX, "", String.class));

        final Mustache mustache = mf.compile("usage.mustache");
        final StringWriter sw = new StringWriter();

        mustache.execute(sw, scopes).close();
        sw.flush();
        in.setBody(sw.toString());

    }
}
