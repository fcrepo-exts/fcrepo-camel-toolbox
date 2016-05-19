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
package org.fcrepo.camel.serialization;

import java.util.Properties;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;

/**
 * Test the route workflow (property 'includeBinaries' is false).
 *
 * @author Bethany Seeger
 * @since 2015-09-28
 */

public abstract class AbstractRouteTest extends CamelBlueprintTestSupport {

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    protected static final String baseURL = "http://localhost/rest";
    protected static final String identifier = "/file1";
    protected static final String auditContainer = "/audit";

    @Override
    public boolean isUseAdviceWith() {
        return true;
    }

    @Override
    public boolean isUseRouteBuilder() {
        return false;
    }

    @Override
    protected String getBlueprintDescriptor() {
        return "/OSGI-INF/blueprint/blueprint.xml";
    }

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        final Properties props = new Properties();

        props.put("serialization.stream", "seda:foo");
        props.put("input.stream", "seda:bar");
        props.put("serialization.format", "RDF_XML");
        props.put("serialization.descriptions", "mock:direct:metadata_file");
        props.put("serialization.binaries", "mock:direct:binary_file");
        // in here to clearly show that we won't include binaries by default
        props.put("serialization.includeBinaries", "false");
        props.put("audit.container", auditContainer);

        return props;
    }
}
