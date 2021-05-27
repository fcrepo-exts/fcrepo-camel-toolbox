package org.fcrepo.camel.ldpathtest;

import org.apache.marmotta.ldpath.api.functions.SelectorFunction;
import org.apache.marmotta.ldpath.model.functions.ConcatenateFunction;
import org.apache.marmotta.ldpath.model.functions.FirstFunction;
import org.fcrepo.camel.ldpath.FcrepoLdPathConfig;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

/**
 * @author dbernstein
 */
@Configuration
public class TestFcrepoLdpathConfig extends FcrepoLdPathConfig {
    protected Set<SelectorFunction> createSelectorFunctions() {
        return Set.of(new ConcatenateFunction(), new FirstFunction());
    }
}