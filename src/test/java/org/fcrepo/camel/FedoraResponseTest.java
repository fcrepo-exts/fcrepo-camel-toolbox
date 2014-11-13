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

import static org.junit.Assert.assertEquals;
import static java.net.URI.create;

import java.net.URI;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author ajs6f
 */
@RunWith(MockitoJUnitRunner.class)
public class FedoraResponseTest {

    @Test
    public void testResponse() {
        final URI uri = create("http://localhost/path/a/b");
        final int status = 200;
        final String contentType = "text/plain";
        final URI location = create("http://localhost/path/a/b/c");
        final String body = "Text response";

        final FedoraResponse response = new FedoraResponse(uri, status, contentType, location, body);

        assertEquals(response.getUrl(), uri);
        assertEquals(response.getStatusCode(), status);
        assertEquals(response.getContentType(), contentType);
        assertEquals(response.getLocation(), location);
        assertEquals(response.getBody(), body);

        response.setUrl(create("http://example.org/path/a/b"));
        assertEquals(response.getUrl(), create("http://example.org/path/a/b"));

        response.setStatusCode(301);
        assertEquals(response.getStatusCode(), 301);

        response.setContentType("application/n-triples");
        assertEquals(response.getContentType(), "application/n-triples");

        response.setLocation(create("http://example.org/path/a/b/c"));
        assertEquals(response.getLocation(), create("http://example.org/path/a/b/c"));

        response.setBody("<http://example.org/book/3> <dc:title> \"Title\" .");
        assertEquals(response.getBody(), "<http://example.org/book/3> <dc:title> \"Title\" .");
    }

}
