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

package org.fcrepo.camel.toolbox.app;

import org.slf4j.Logger;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.concurrent.Callable;

import static org.fcrepo.camel.common.config.BasePropsConfig.FCREPO_CAMEL_CONFIG_FILE_PROP_SOURCE;
import static org.slf4j.LoggerFactory.getLogger;

//TODO pull in version and git revision from generated property file

/**
 * The command line tool entry point and parameter definitions
 *
 * @author dbernstein
 */
@CommandLine.Command(name = "fcrepo-camel-toolbox",
        mixinStandardHelpOptions = true, sortOptions = false,
        versionProvider = AppVersionProvider.class)
public class Driver implements Callable<Integer> {

    private static final Logger LOGGER = getLogger(Driver.class);

    @CommandLine.Option(names = {"--config", "-c"}, required = false, order = 1,
            description = "The path to the configuration file")
    private Path configurationFilePath;

    @Override
    public Integer call() {

        if (configurationFilePath != null) {
            System.setProperty(FCREPO_CAMEL_CONFIG_FILE_PROP_SOURCE, configurationFilePath.toFile().getAbsolutePath());
        }
        final var appContext = new AnnotationConfigApplicationContext("org.fcrepo.camel");
        appContext.start();
        LOGGER.info("fcrepo-camel-toolbox started.");

        while (appContext.isRunning()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException("This should never happen");
            }
        }
        return 0;
    }

    /**
     * @param args Command line arguments
     */
    public static void main(final String[] args) {
        final Driver driver = new Driver();
        final CommandLine cmd = new CommandLine(driver);
        cmd.setExecutionExceptionHandler(new AppExceptionHandler(driver));
        cmd.execute(args);
    }

    private static class AppExceptionHandler implements CommandLine.IExecutionExceptionHandler {

        private final Driver driver;

        AppExceptionHandler(final Driver driver) {
            this.driver = driver;
        }

        @Override
        public int handleExecutionException(
                final Exception ex,
                final CommandLine commandLine,
                final CommandLine.ParseResult parseResult) {
            commandLine.getErr().println(ex.getMessage());
            ex.printStackTrace(commandLine.getErr());
            commandLine.usage(commandLine.getErr());
            return commandLine.getCommandSpec().exitCodeOnExecutionException();
        }
    }

}
