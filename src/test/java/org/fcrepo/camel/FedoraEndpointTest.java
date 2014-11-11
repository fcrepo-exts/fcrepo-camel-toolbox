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

    @Mock
    private FedoraComponent mockContext;

    @Mock
    private Processor mockProcessor;

    @Test(expected = RuntimeCamelException.class)
    public void testNoConsumerCanBeCreated() {
        final FedoraEndpoint testEndpoint = new FedoraEndpoint("", "", mockContext);
        testEndpoint.createConsumer(mockProcessor);
    }

    @Test
    public void testCreateProducer() {
        final FedoraEndpoint testEndpoint = new FedoraEndpoint("", "", mockContext);
        final Producer testProducer = testEndpoint.createProducer();
        assertEquals(testEndpoint, testProducer.getEndpoint());
    }

}
