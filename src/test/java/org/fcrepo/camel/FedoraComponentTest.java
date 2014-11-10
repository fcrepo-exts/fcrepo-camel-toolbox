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

import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author ajs6f
 */
@RunWith(MockitoJUnitRunner.class)
public class FedoraComponentTest {

    private static final String TEST_ENDPOINT_URI = "fcrepo:foo";

    private static final Map<String, Object> EMPTY_MAP = emptyMap();

    @Mock
    private CamelContext mockContext;

    @Test
    public void testCreateEndpoint() throws Exception {
        final FedoraComponent testComponent = new FedoraComponent(mockContext);
        final Endpoint testEndpoint = testComponent.createEndpoint(TEST_ENDPOINT_URI, "", EMPTY_MAP);
        assertEquals(mockContext, testEndpoint.getCamelContext());
        assertEquals(TEST_ENDPOINT_URI, testEndpoint.getEndpointUri());
    }

}
