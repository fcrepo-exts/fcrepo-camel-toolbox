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

import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.RuntimeCamelException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author ajs6f
 */
@RunWith(MockitoJUnitRunner.class)
public class FedoraEndpointTest {

    private final String FCREPO_URI = "fcrepo:foo";

    private final String FCREPO_PATH = "/rest";

    @Mock
    private FedoraComponent mockContext;

    @Mock
    private Processor mockProcessor;

    @Test(expected = RuntimeCamelException.class)
    public void testNoConsumerCanBeCreated() throws RuntimeCamelException {
        final FedoraEndpoint testEndpoint = new FedoraEndpoint(FCREPO_URI, FCREPO_PATH, mockContext);
        testEndpoint.createConsumer(mockProcessor);
    }

    @Test
    public void testCreateProducer() {
        final FedoraEndpoint testEndpoint = new FedoraEndpoint(FCREPO_URI, FCREPO_PATH, mockContext);
        final Producer testProducer = testEndpoint.createProducer();
        assertEquals(testEndpoint, testProducer.getEndpoint());
    }

    @Test
    public void testBaseUrl() {
        final FedoraEndpoint testEndpoint = new FedoraEndpoint(FCREPO_URI, FCREPO_PATH, mockContext);
        assertEquals(testEndpoint.getBaseUrl(), FCREPO_PATH);
    }

    @Test
    public void testTombstone() {
        final FedoraEndpoint testEndpoint = new FedoraEndpoint(FCREPO_URI, FCREPO_PATH, mockContext);
        assertEquals(false, testEndpoint.getTombstone());
        testEndpoint.setTombstone("true");
        assertEquals(true, testEndpoint.getTombstone());
    }

    @Test
    public void testTransform() {
        final FedoraEndpoint testEndpoint = new FedoraEndpoint(FCREPO_URI, FCREPO_PATH, mockContext);
        final String transform = "default";
        assertEquals(null, testEndpoint.getTransform());
        testEndpoint.setTransform(transform);
        assertEquals(transform, testEndpoint.getTransform());
    }

    @Test
    public void testThrowExceptionOnFailure() {
        final FedoraEndpoint testEndpoint = new FedoraEndpoint(FCREPO_URI, FCREPO_PATH, mockContext);
        assertEquals(true, testEndpoint.getThrowExceptionOnFailure());
        testEndpoint.setThrowExceptionOnFailure("false");
        assertEquals(false, testEndpoint.getThrowExceptionOnFailure());
    }

    @Test
    public void testMetadata() {
        final FedoraEndpoint testEndpoint = new FedoraEndpoint(FCREPO_URI, FCREPO_PATH, mockContext);
        assertEquals(true, testEndpoint.getMetadata());
        testEndpoint.setMetadata("false");
        assertEquals(false, testEndpoint.getMetadata());
    }

    @Test
    public void testAuthHost() {
        final FedoraEndpoint testEndpoint = new FedoraEndpoint(FCREPO_URI, FCREPO_PATH, mockContext);
        final String authHost = "example.org";
        assertEquals(null, testEndpoint.getAuthHost());
        testEndpoint.setAuthHost(authHost);
        assertEquals(authHost, testEndpoint.getAuthHost());
    }

    @Test
    public void testAuthUser() {
        final FedoraEndpoint testEndpoint = new FedoraEndpoint(FCREPO_URI, FCREPO_PATH, mockContext);
        final String authUser = "fedoraAdmin";
        assertEquals(null, testEndpoint.getAuthUsername());
        testEndpoint.setAuthUsername(authUser);
        assertEquals(authUser, testEndpoint.getAuthUsername());
    }

    @Test
    public void testAuthPassword() {
        final FedoraEndpoint testEndpoint = new FedoraEndpoint(FCREPO_URI, FCREPO_PATH, mockContext);
        final String authPassword = "foo";
        assertEquals(null, testEndpoint.getAuthPassword());
        testEndpoint.setAuthPassword(authPassword);
        assertEquals(authPassword, testEndpoint.getAuthPassword());
    }

    @Test
    public void testAccept() {
        final FedoraEndpoint testEndpoint = new FedoraEndpoint(FCREPO_URI, FCREPO_PATH, mockContext);
        final String accept1 = "application/rdf+xml";
        final String accept2 = "text/turtle";
        final String accept3 = "application/ld+json";
        final String accept4 = "application/sparql-update";
        assertEquals(null, testEndpoint.getAccept());
        testEndpoint.setAccept("application/rdf xml");
        assertEquals(accept1, testEndpoint.getAccept());
        testEndpoint.setAccept(accept2);
        assertEquals(accept2, testEndpoint.getAccept());
        testEndpoint.setAccept(accept3);
        assertEquals(accept3, testEndpoint.getAccept());
        testEndpoint.setAccept(accept4);
        assertEquals(accept4, testEndpoint.getAccept());
    }

    @Test
    public void testContentType() {
        final FedoraEndpoint testEndpoint = new FedoraEndpoint(FCREPO_URI, FCREPO_PATH, mockContext);
        final String contentType1 = "application/rdf+xml";
        final String contentType2 = "text/turtle";
        final String contentType3 = "application/ld+json";
        final String contentType4 = "application/sparql-update";
        assertEquals(null, testEndpoint.getContentType());
        testEndpoint.setContentType("application/rdf xml");
        assertEquals(contentType1, testEndpoint.getContentType());
        testEndpoint.setContentType(contentType2);
        assertEquals(contentType2, testEndpoint.getContentType());
        testEndpoint.setContentType(contentType3);
        assertEquals(contentType3, testEndpoint.getContentType());
        testEndpoint.setContentType(contentType4);
        assertEquals(contentType4, testEndpoint.getContentType());
    }

    @Test
    public void testConstants() {
        assertEquals(FedoraEndpoint.FCREPO_BASE_URL, "FCREPO_BASE_URL");
        assertEquals(FedoraEndpoint.DEFAULT_CONTENT_TYPE, "application/rdf+xml");
        assertEquals(FedoraEndpoint.FCREPO_IDENTIFIER, "FCREPO_IDENTIFIER");
    }

    @Test
    public void testSingleton() {
        final FedoraEndpoint testEndpoint = new FedoraEndpoint(FCREPO_URI, FCREPO_PATH, mockContext);
        assertEquals(true, testEndpoint.isSingleton());
    }

}
