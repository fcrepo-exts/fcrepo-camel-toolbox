/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

package org.fcrepo.camel.toolbox.app;

import org.slf4j.Logger;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import static org.fcrepo.camel.common.config.BasePropsConfig.FCREPO_CAMEL_CONFIG_FILE_PROPERTY;
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
            System.setProperty(FCREPO_CAMEL_CONFIG_FILE_PROPERTY, configurationFilePath.toFile().getAbsolutePath());
        }
        final var appContext = new AnnotationConfigApplicationContext("org.fcrepo.camel");
        final var countdownLatch = new CountDownLatch(1);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutting down fcrepo-camel-toolbox...");
            appContext.stop();
            countdownLatch.countDown();
        }));
        appContext.start();
        LOGGER.info("fcrepo-camel-toolbox started.");
        try {
            countdownLatch.await();
        } catch (final InterruptedException e) {
            // Ignore error because we are exiting anyways.
            return 1;
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
