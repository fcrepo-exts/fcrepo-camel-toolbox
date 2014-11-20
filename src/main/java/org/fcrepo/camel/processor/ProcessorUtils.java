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

import static org.fcrepo.camel.FedoraEndpoint.FCREPO_BASE_URL;
import static org.fcrepo.camel.FedoraEndpoint.FCREPO_IDENTIFIER;
import static org.fcrepo.jms.headers.DefaultMessageFactory.BASE_URL_HEADER_NAME;
import static org.fcrepo.jms.headers.DefaultMessageFactory.IDENTIFIER_HEADER_NAME;

import java.io.IOException;

import org.apache.camel.Message;

/**
 * Utility functions for fcrepo processor classes
 * @author Aaron Coburn
 * @since November 14, 2014
 */

public final class ProcessorUtils {

    /**
     * This is a utility class; the constructor is off-limits.
     */
    private ProcessorUtils() {
    }

    /**
     * Extract the subject URI from the incoming message headers.
     * @param in the incoming Message
     */
    public static String getSubjectUri(final Message in) throws IOException {
        final StringBuilder base = new StringBuilder("");

        if (in.getHeader(FCREPO_BASE_URL) != null) {
            base.append(in.getHeader(FCREPO_BASE_URL, String.class));
        } else if (in.getHeader(BASE_URL_HEADER_NAME) != null) {
            base.append(in.getHeader(BASE_URL_HEADER_NAME, String.class));
        } else {
            throw new IOException("No baseURL header available!");
        }

        if (in.getHeader(FCREPO_IDENTIFIER) != null) {
           base.append(in.getHeader(FCREPO_IDENTIFIER));
        } else if (in.getHeader(IDENTIFIER_HEADER_NAME) != null) {
           base.append(in.getHeader(IDENTIFIER_HEADER_NAME));
        }
        return base.toString();
    }
}

