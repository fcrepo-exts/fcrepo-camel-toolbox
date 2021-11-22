/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
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
 * A configuration class for the Fixity service
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

    @Value("${fixity.input.stream:broker:queue:fixity}")
    private String inputStream;


    @Value("${fixity.delay:0}")
    private long fixityDelay;

    @Value("${fixity.failure:file:/tmp/?fileName=fixityErrors.log&fileExist=Append}")
    private String fixityFailure;


    @Value("${fixity.success:mock:fixity.success}")
    private String fixitySuccess;

    /**
     * The jms message stream for the fixity service
     * @return
     */
    public String getInputStream() {
        return inputStream;
    }

    /**
     * Because fixity checking can put a significant load on a server, it can be convenient
     * to introduce a delay between each fixity check. That delay is measured in milliseconds.
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
