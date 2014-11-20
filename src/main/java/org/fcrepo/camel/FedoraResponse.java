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
package org.fcrepo.camel;

import java.net.URI;

/**
 * Represents a response from a fedora repository using a {@link FedoraClient}.
 *
 * Note: This should be swapped out to use https://github.com/fcrepo4-labs/fcrepo4-client
 *
 * @author Aaron Coburn
 * @since October 20, 2014
 */
public class FedoraResponse {

    private URI url;

    private int statusCode;

    private URI location;

    private String body;

    private String contentType;

    /**
     * Create a FedoraResponse object from the http response
     */
    public FedoraResponse(final URI url, final int statusCode,
            final String contentType, final URI location, final String body) {
        this.setUrl(url);
        this.setStatusCode(statusCode);
        this.setLocation(location);
        this.setContentType(contentType);
        this.setBody(body);
    }

    /**
     * url getter
     */
    public URI getUrl() {
        return url;
    }

    /**
     * url setter
     * @param url the URL
     */
    public void setUrl(final URI url) {
        this.url = url;
    }

    /**
     * statusCode getter
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * statusCode setter
     * @param statusCode the http status code
     */
    public void setStatusCode(final int statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * body getter
     */
    public String getBody() {
        return body;
    }

    /**
     * body setter
     * @param body the contents of the response body
     */
    public void setBody(final String body) {
        this.body = body;
    }

    /**
     * location getter
     */
    public URI getLocation() {
        return location;
    }

    /**
     * location setter
     * @param location the value of any Link: rel=describedby header
     */
    public void setLocation(final URI location) {
        this.location = location;
    }

    /**
     * contentType getter
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * contentType setter
     * @param contentType the Content-Type of the response
     */
    public void setContentType(final String contentType) {
        this.contentType = contentType;
    }
}
