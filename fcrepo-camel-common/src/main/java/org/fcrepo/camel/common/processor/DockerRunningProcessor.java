/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel.common.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.nio.file.Path;

/**
 * Processor for determining if the application is currently running in
 * a Docker environment. Adds a header `CamelDockerRunning` of type boolean
 * to the exchange headers.
 *
 * WARNING
 * Checks for existence of /.dockerenv in the filesystem. Note that the presence
 * of this file is not documented and that this check might not work in the future.
 *
 * See <a href="https://superuser.com/questions/1021834/what-are-dockerenv-and-dockerinit">...</a>
 *
 * @author Ralf Claussnitzer
 */
public class DockerRunningProcessor implements Processor {

    public static final String DOCKER_RUNNING = "CamelDockerRunning";

    @Override
    public void process(Exchange exchange) throws Exception {
        boolean dockerenvExists = Path.of("/.dockerenv").toFile().exists();
        exchange.getMessage().setHeader(DOCKER_RUNNING, dockerenvExists);
    }
}
