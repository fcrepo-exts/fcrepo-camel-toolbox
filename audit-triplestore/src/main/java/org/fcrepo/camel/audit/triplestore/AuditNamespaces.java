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

/**
 * @author acoburn
 */
public final class AuditNamespaces {

    public static final String AUDIT = "http://fedora.info/definitions/v4/audit#";
    public static final String EVENT_TYPE = "http://id.loc.gov/vocabulary/preservation/eventType/";
    public static final String PREMIS = "http://www.loc.gov/premis/rdf/v1#";
    public static final String PROV = "http://www.w3.org/ns/prov#";
    public static final String XSD = "http://www.w3.org/2001/XMLSchema#";

    private AuditNamespaces() {
        // prevent instantiation
    }
}
