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
package org.fcrepo.camel.service;

import org.fcrepo.camel.FcrepoComponent;
import org.fcrepo.camel.common.config.BasePropsConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author dbernstein
 */
@Configuration
public class FcrepoCamelConfig extends BasePropsConfig {


    @Value("${fcrepo.baseUrl:http://localhost:8080/fcrepo/rest}")
    private String fcrepoBaseUrl;

    @Value("${fcrepo.authUsername:fedoraAdmin}")
    private String fcrepoUsername;

    @Value("${fcrepo.authPassword:fedoraAdmin}")
    private String fcrepoPassword;

    @Value("${fcrepo.authHost:#{null}}")
    private String fcrepoAuthHost;

    @Bean("fcrepo")
    public FcrepoComponent fcrepoComponent() {
        final var fcrepoComponent = new FcrepoComponent();
        fcrepoComponent.setBaseUrl(fcrepoBaseUrl);
        fcrepoComponent.setAuthUsername(fcrepoUsername);
        fcrepoComponent.setAuthPassword(fcrepoPassword);
        fcrepoComponent.setAuthHost(fcrepoAuthHost);
        return fcrepoComponent;
    }

    public String getFcrepoBaseUrl() {
        return fcrepoBaseUrl;
    }
}
