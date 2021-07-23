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

import picocli.CommandLine;

import java.util.Properties;

/**
 * @author dbernstein
 */
class AppVersionProvider implements CommandLine.IVersionProvider {
    @Override
    public String[] getVersion() throws Exception {
            try {
                final var is = AppVersionProvider.class.getResourceAsStream("app.properties");
                final var appProps = new Properties();
                appProps.load(is);
                return new String[]{appProps.get("app.version") + " r." + appProps.get("app.revision")};
            } catch (final Exception ex) {
                ex.printStackTrace();
                throw new RuntimeException("This should never happen");
            }
    }
}
