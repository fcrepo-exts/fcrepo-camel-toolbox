/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.camel.toolbox.app;

import picocli.CommandLine;

import java.util.Properties;

/**
 * @author dbernstein
 */
class AppVersionProvider implements CommandLine.IVersionProvider {
    @Override
    public String[] getVersion() throws Exception {
            try {
                final var is = AppVersionProvider.class.getResourceAsStream("/app.properties");
                final var appProps = new Properties();
                appProps.load(is);
                return new String[]{appProps.get("app.version") + " r." + appProps.get("app.revision")};
            } catch (final Exception ex) {
                ex.printStackTrace();
                throw new RuntimeException("This should never happen");
            }
    }
}
