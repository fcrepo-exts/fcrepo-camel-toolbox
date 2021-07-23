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

package org.fcrepo.camel.common.config;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

/**
 * A base class for property configs
 *
 * @author dbernstein
 */
@PropertySources({
        @PropertySource(value = BasePropsConfig.DEFAULT_FCREPO_CAMEL_TOOLBOX_CONFIG_FILE_PROP_SOURCE,
                ignoreResourceNotFound = true),
        @PropertySource(value = BasePropsConfig.FCREPO_CAMEL_CONFIG_FILE_PROP_SOURCE, ignoreResourceNotFound = true)
})
public abstract class BasePropsConfig {

    public static final String FCREPO_CAMEL_TOOLBOX_HOME_PROPERTY = "fcrepo.home";
    public static final String DEFAULT_FCREPO_HOME_VALUE = "fcrepo-camel-toolbox-home";
    public static final String DEFAULT_FCREPO_CAMEL_TOOLBOX_CONFIG_FILE_PROP_SOURCE =
            "file:${" + FCREPO_CAMEL_TOOLBOX_HOME_PROPERTY + ":" + DEFAULT_FCREPO_HOME_VALUE + "}/config/fcrepo-camel-toolbox.properties";
    public static final String FCREPO_CAMEL_CONFIG_FILE_PROP_SOURCE = "file:${fcrepo.camel.toolbox.config.file}";

    protected Path createDirectories(final Path path) throws IOException {
        try {
            return Files.createDirectories(path);
        } catch (final FileAlreadyExistsException e) {
            // Ignore. This only happens with the path is a symlink
            return path;
        }
    }

}
