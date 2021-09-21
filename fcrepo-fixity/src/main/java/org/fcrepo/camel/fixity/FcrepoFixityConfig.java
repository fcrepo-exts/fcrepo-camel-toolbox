/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.camel.fixity;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http.HttpComponent;
import org.fcrepo.camel.common.config.BasePropsConfig;
import org.fcrepo.camel.common.config.ConditionOnPropertyTrue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * A configuration class for the Solr Indexer service
 *
 * @author dbernstein
 */
@Configuration
@Conditional({FcrepoFixityConfig.FixityEnabled.class})
public class FcrepoFixityConfig extends BasePropsConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(FcrepoFixityConfig.class);
    static final String FIXITY_ENABLED = "fixity.enabled";

    static class FixityEnabled extends ConditionOnPropertyTrue {
        FixityEnabled() {
            super(FcrepoFixityConfig.FIXITY_ENABLED, false);
        }
    }

    @Value("${fixity.stream:broker:topic:fixity}")
    private String fixityStream;


    @Value("${fixity.delay:0}")
    private long fixityDelay;

    @Value("${fixity.failure:file:/tmp/?fileName=fixityErrors.log&fileExist=Append}")
    private String fixityFailure;


    @Value("${fixity.success:mock:fixity.success}")
    private String fixitySuccess;

    /**
     * The input stream for the fixity service
     * @return
     */
    public String getFixityStream() {
        return fixityStream;
    }

    /**
     * Because fixity checking can put a significant load on a server, it can be convenient
     * o introduce a delay between each fixity check. That delay is measured in milliseconds.
     */
    public long getFixityDelay() {
        return fixityDelay;
    }

    /**
     * It is also possible to trigger an action on success (by default, this is a no-op):
     */
    public String getFixitySuccess() {
        return fixitySuccess;
    }

    /**
     * Most importantly, it is possible to configure what should happen when a fixity check fails.
     * In the default example below, the fixity output is written to a file in `/tmp/fixityErrors.log`. But this can
     * be changed to send a message to an email address (`fixity.failure=smtp:admin@example.org?subject=Fixity`)
     * or use just about any other camel component.
     */
    public String getFixityFailure() {
        return fixityFailure;
    }

    @Bean(name = "http")
    public HttpComponent http() {
        return new HttpComponent();
    }

    @Bean(name = "https")
    public HttpComponent https() {
        return new HttpComponent();
    }

    @Bean
    public RouteBuilder fixityRoute() {
        return new FixityRouter();
    }
}
