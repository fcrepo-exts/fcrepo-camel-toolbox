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
package org.fcrepo.camel.integration;

import static java.lang.Integer.parseInt;
import static java.lang.System.getProperty;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.jena.fuseki.FusekiCmd;
import org.fcrepo.http.commons.test.util.ContainerWrapper;
import org.slf4j.Logger;

/**
 * This class starts/stops Fuseki along with the test Grizzly container.
 *
 * @author Andrew Woods
 *         Date: 8/22/13
 */
public class FusekiContainerWrapper extends ContainerWrapper {

    final private Logger logger = getLogger(FusekiContainerWrapper.class);

    private static final int FUSEKI_PORT = parseInt(getProperty(
            "test.fuseki.port", "3030"));

    private static final int MGT_PORT = parseInt(getProperty("test.mgt.port",
            "3031"));

    private static final String serverAddress = "http://localhost:" + MGT_PORT
            + "/mgt";

    private static CloseableHttpClient client;

    @Override
    @PostConstruct
    public void start() throws Exception {

        client = HttpClients.createDefault();

        final HttpGet method = new HttpGet(serverAddress);
        try {
            client.execute(method);

        } catch (final IOException e) {
            logger.debug("starting Fuseki");
            new Thread(new FusekiRunner()).start();
        }

        super.start();
    }

    /**
     * This class runs Fuseki in a non-blocking thread.
     */
    private class FusekiRunner implements Runnable {
        @Override
        public void run() {
            FusekiCmd.main("--update",
                           "--mem",
                           "--port=" + FUSEKI_PORT,
                           "--mgtPort=" + MGT_PORT,
                           "/test");
        }
    }

    @Override
    @PreDestroy
    public void stop() {
        super.stop();

        final HttpPost method = new HttpPost(serverAddress + "?cmd=shutdown");
        try {
            logger.debug("stopping Fuseki");
            client.execute(method);
        } catch (final IOException e) {
            // do nothing
        }
    }

}
