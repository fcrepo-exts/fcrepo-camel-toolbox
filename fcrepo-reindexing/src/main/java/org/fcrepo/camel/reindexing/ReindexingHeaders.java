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
package org.fcrepo.camel.reindexing;

/**
 * A class defining header values for the indexing routes
 * 
 * @author acoburn
 * @since May 22, 2015
 */
public final class ReindexingHeaders {
    public static final String REST_PORT = "CamelFcrepoReindexingRestPort";
    public static final String REST_PREFIX = "CamelFcrepoReindexingRestPrefix";
    public static final String RECIPIENTS = "CamelFcrepoReindexingRecipients";

    private ReindexingHeaders() {
        // prevent instantiation
    }
}
