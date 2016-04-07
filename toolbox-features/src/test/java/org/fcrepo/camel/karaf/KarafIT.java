/**
 * Copyright 2015 DuraSpace, Inc.
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
package org.fcrepo.camel.karaf;

import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.impl.client.HttpClientBuilder.create;
import static org.slf4j.LoggerFactory.getLogger;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureConsole;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;

import java.io.File;

import javax.inject.Inject;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.karaf.features.FeaturesService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.ConfigurationManager;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.slf4j.Logger;

/**
 * @author Aaron Coburn
 * @since February 8, 2016
 */
@RunWith(PaxExam.class)
public class KarafIT {

    private static Logger LOG = getLogger(KarafIT.class);

    @Inject
    protected FeaturesService featuresService;

    @Configuration
    public Option[] config() {
        final ConfigurationManager cm = new ConfigurationManager();
        final String fcrepoPort = cm.getProperty("fcrepo.dynamic.test.port");
        final String jmsPort = cm.getProperty("fcrepo.dynamic.jms.port");
        final String reindexingPort = cm.getProperty("fcrepo.dynamic.reindexing.port");
        final String rmiRegistryPort = cm.getProperty("karaf.rmiRegistry.port");
        final String rmiServerPort = cm.getProperty("karaf.rmiServer.port");
        final String sshPort = cm.getProperty("karaf.ssh.port");
        final String fcrepoFeatures = "file:" + cm.getProperty("project.build.outputDirectory") + "/features.xml";
        return new Option[] {
            karafDistributionConfiguration()
                .frameworkUrl(maven().groupId("org.apache.karaf").artifactId("apache-karaf")
                        .versionAsInProject().type("zip"))
                .unpackDirectory(new File("target", "exam"))
                .useDeployFolder(false),
            logLevel(LogLevel.WARN),
            keepRuntimeFolder(),
            configureConsole().ignoreLocalConsole(),
            features(maven().groupId("org.apache.karaf.features").artifactId("standard")
                        .versionAsInProject().classifier("features").type("xml"), "scr"),
            features(maven().groupId("org.apache.camel.karaf").artifactId("apache-camel")
                        .type("xml").classifier("features").versionAsInProject(), "camel-blueprint"),
            features(bundle(fcrepoFeatures).start(), "fcrepo-indexing-triplestore",
                    "fcrepo-indexing-solr", "fcrepo-reindexing", "fcrepo-serialization",
                    "fcrepo-fixity", "fcrepo-audit-triplestore"),
            systemProperty("karaf.reindexing.port").value(reindexingPort),
            editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiRegistryPort", rmiRegistryPort),
            editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiServerPort", rmiServerPort),
            editConfigurationFilePut("etc/org.apache.karaf.shell.cfg", "sshPort", sshPort),
            editConfigurationFilePut("etc/org.fcrepo.camel.indexing.triplestore.cfg", "fcrepo.baseUrl", "localhost:" + fcrepoPort + "/fcrepo/rest"),
            editConfigurationFilePut("etc/org.fcrepo.camel.indexing.triplestore.cfg", "jms.brokerUrl", "tcp://localhost:" + jmsPort),
            editConfigurationFilePut("etc/org.fcrepo.camel.indexing.solr.cfg", "fcrepo.baseUrl", "localhost:" + fcrepoPort + "/fcrepo/rest"),
            editConfigurationFilePut("etc/org.fcrepo.camel.indexing.solr.cfg", "jms.brokerUrl", "tcp://localhost:" + jmsPort),
            editConfigurationFilePut("etc/org.fcrepo.camel.reindexing.cfg", "fcrepo.baseUrl", "localhost:" + fcrepoPort + "/fcrepo/rest"),
            editConfigurationFilePut("etc/org.fcrepo.camel.reindexing.cfg", "jms.brokerUrl", "tcp://localhost:" + jmsPort),
            editConfigurationFilePut("etc/org.fcrepo.camel.reindexing.cfg", "rest.port", reindexingPort),
            editConfigurationFilePut("etc/org.fcrepo.camel.serialization.cfg", "jms.brokerUrl", "tcp://localhost:" + jmsPort),
            editConfigurationFilePut("etc/org.fcrepo.camel.audit.triplestore.cfg", "jms.brokerUrl", "tcp://localhost:" + jmsPort),
            editConfigurationFilePut("etc/org.fcrepo.camel.fixity.cfg", "jms.brokerUrl", "tcp://localhost:" + jmsPort),
       };
    }

    @Test
    public void testInstallation() throws Exception {
        assertTrue(featuresService.isInstalled(featuresService.getFeature("camel-core")));
        assertTrue(featuresService.isInstalled(featuresService.getFeature("fcrepo-camel")));
        assertTrue(featuresService.isInstalled(featuresService.getFeature("fcrepo-indexing-triplestore")));
        assertTrue(featuresService.isInstalled(featuresService.getFeature("fcrepo-indexing-solr")));
        assertTrue(featuresService.isInstalled(featuresService.getFeature("fcrepo-reindexing")));
        assertTrue(featuresService.isInstalled(featuresService.getFeature("fcrepo-serialization")));
        assertTrue(featuresService.isInstalled(featuresService.getFeature("fcrepo-audit-triplestore")));
        assertTrue(featuresService.isInstalled(featuresService.getFeature("fcrepo-fixity")));
    }

    @Test
    public void testReindexingService() throws Exception {
        final CloseableHttpClient client = create().build();
        final String reindexingUrl = "http://localhost:" + System.getProperty("karaf.reindexing.port") + "/reindexing/";
        try (final CloseableHttpResponse response = client.execute(new HttpGet(reindexingUrl))) {
            assertEquals(SC_OK, response.getStatusLine().getStatusCode());
        }

        final HttpPost post = new HttpPost(reindexingUrl);
        post.addHeader("Content-Type", "application/json");
        post.setEntity(new StringEntity("[\"log:fcrepo\"]"));
        try (final CloseableHttpResponse response = client.execute(post)) {
            assertEquals(SC_OK, response.getStatusLine().getStatusCode());
        }
    }
}
