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
package org.fcrepo.camel.service.activemq;

import static org.apache.camel.component.mock.MockEndpoint.assertIsSatisfied;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.impl.client.HttpClientBuilder.create;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.streamBundle;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureConsole;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;
import static org.ops4j.pax.exam.util.PathUtils.getBaseDir;
import static org.osgi.framework.Bundle.ACTIVE;
import static org.osgi.framework.Constants.OBJECTCLASS;
import static org.osgi.framework.FrameworkUtil.createFilter;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import javax.inject.Inject;

import org.apache.camel.CamelContext;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.karaf.features.FeaturesService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.ConfigurationManager;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;

/**
 * @author Aaron Coburn
 * @since May 4, 2016
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class KarafIT {

    private static Logger LOGGER = getLogger(KarafIT.class);

    @Inject
    protected FeaturesService featuresService;

    @Inject
    protected BundleContext bundleContext;

    @Configuration
    public Option[] config() throws Exception {
        final ConfigurationManager cm = new ConfigurationManager();
        final String artifactName = cm.getProperty("project.artifactId") + "-" + cm.getProperty("project.version");
        final String fcrepoServiceBundle = "file:" + getBaseDir() + "/target/" + artifactName + ".jar";
        final String fcrepoPort = cm.getProperty("fcrepo.dynamic.test.port");
        final String jmsPort = cm.getProperty("fcrepo.dynamic.jms.port");
        final String rmiRegistryPort = cm.getProperty("karaf.rmiRegistry.port");
        final String rmiServerPort = cm.getProperty("karaf.rmiServer.port");
        final String sshPort = cm.getProperty("karaf.ssh.port");
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
                        .type("xml").classifier("features").versionAsInProject(), "scr"),
            features(maven().groupId("org.apache.camel.karaf").artifactId("apache-camel")
                        .type("xml").classifier("features").versionAsInProject(), "camel",
                        "camel-blueprint", "camel-http4", "camel-jms"),
            features(maven().groupId("org.apache.activemq").artifactId("activemq-karaf")
                        .type("xml").classifier("features").versionAsInProject(), "activemq-camel"),
            features(maven().groupId("org.fcrepo.camel").artifactId("fcrepo-camel")
                        .type("xml").classifier("features").versionAsInProject(), "fcrepo-camel"),

            CoreOptions.systemProperty("fcrepo.port").value(fcrepoPort),
            CoreOptions.systemProperty("jms.port").value(jmsPort),
            CoreOptions.systemProperty("fcrepo.service.activemq.bundle").value(fcrepoServiceBundle),

            editConfigurationFilePut("etc/org.fcrepo.camel.service.activemq.cfg", "jms.brokerUrl",
                    "tcp://localhost:" + jmsPort),

            bundle(fcrepoServiceBundle).start(),
            streamBundle(
                    TinyBundles.bundle().add("OSGI-INF/blueprint/blueprint-test.xml",
                    new File("src/test/resources/OSGI-INF/blueprint/blueprint-test.xml").toURL())
                    .set(Constants.BUNDLE_SYMBOLICNAME, "org.fcrepo.camel.service.activemq.test")
                    .set(Constants.BUNDLE_MANIFESTVERSION, "2")
                    .set(Constants.DYNAMICIMPORT_PACKAGE, "*")
                    .build()
                ).start(),

            editConfigurationFilePut("etc/org.fcrepo.camel.service.activemq.cfg", "jms.consumers", "1"),
            editConfigurationFilePut("etc/org.fcrepo.camel.service.activemq.cfg", "jms.connections", "1"),
            editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiRegistryPort", rmiRegistryPort),
            editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiServerPort", rmiServerPort),
            editConfigurationFilePut("etc/org.apache.karaf.shell.cfg", "sshPort", sshPort)
       };
    }

    @Test
    public void testInstallation() throws Exception {
        assertTrue(featuresService.isInstalled(featuresService.getFeature("camel-core")));
        assertNotNull(bundleContext);
        assertEquals(ACTIVE, bundleContext.getBundle(System.getProperty("fcrepo.service.activemq.bundle")).getState());
    }

    @Test
    public void testQueuingService() throws Exception {
        final CloseableHttpClient client = create().build();
        final String baseUrl = "http://localhost:" + System.getProperty("fcrepo.port") + "/fcrepo/rest";
        final CamelContext ctx = getOsgiService(CamelContext.class,
                "(camel.context.name=FcrepoQueuingService)", 10000);

        assertNotNull(ctx);

        final MockEndpoint resultEndpoint = (MockEndpoint) ctx.getEndpoint("mock:result");
        resultEndpoint.reset();

        final String url1 = post(baseUrl);
        final String url2 = post(baseUrl);

        resultEndpoint.expectedMessageCount(4);
        resultEndpoint.expectedHeaderValuesReceivedInAnyOrder(FCREPO_URI, url1, url2, baseUrl, baseUrl);
        assertIsSatisfied(resultEndpoint);
    }

    private String post(final String url) {
        final CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            final HttpPost httppost = new HttpPost(url);
            final HttpResponse response = httpclient.execute(httppost);
            assertEquals(SC_CREATED, response.getStatusLine().getStatusCode());
            return EntityUtils.toString(response.getEntity(), "UTF-8");
        } catch (IOException ex) {
            LOGGER.debug("Unable to extract HttpEntity response into an InputStream: ", ex);
            return "";
        }
    }

    private <T> T getOsgiService(final Class<T> type, final String filter, final long timeout) {
        try {
            final ServiceTracker tracker = new ServiceTracker(bundleContext,
                    createFilter("(&(" + OBJECTCLASS + "=" + type.getName() + ")" + filter + ")"), null);
            tracker.open(true);
            final Object svc = type.cast(tracker.waitForService(timeout));
            if (svc == null) {
                throw new RuntimeException("Gave up waiting for service " + filter);
            }
            return type.cast(svc);
        } catch (InvalidSyntaxException e) {
            throw new IllegalArgumentException("Invalid filter", e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
