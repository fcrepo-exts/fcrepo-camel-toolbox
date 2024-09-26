/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel.common;

import org.apache.camel.impl.engine.DefaultTracer;
import org.slf4j.Logger;

/**
 * A test tracer for logging camel routes under our control.
 * @author whikloj
 *
 * To use, before adapting the context add camelContext.setTracer(new TestTracer(logger));
 * i.e.
 *  final Logger LOGGER = LoggerFactory.getLogger(FcrepoSolrIndexer.class);
 *  camelContext.setTracer(new TestTracer(LOGGER));
 *  final var context = camelContext.adapt(ModelCamelContext.class);
 *  AdviceWith.adviceWith(context, "FcrepoSolrIndexer", a -%lt; {
 */
public class TestTracer extends DefaultTracer {
    private final Logger LOGGER;

    public TestTracer(final Logger logger) {
        super();
        this.LOGGER = logger;
        this.setTracePattern("%-4.4s [%-20.20s] [%-40.40s]");
    }

    @Override
    protected void dumpTrace(final String out) {
        LOGGER.info(out);
    }
}
