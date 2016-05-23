/*
 * Copyright 2016 DuraSpace, Inc.
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

import static java.net.URI.create;
import static org.apache.camel.component.mock.MockEndpoint.assertIsSatisfied;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.impl.client.HttpClients.createDefault;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
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
import java.net.URI;
import javax.inject.Inject;

import org.apache.camel.CamelContext;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.karaf.features.FeaturesService;
import org.fcrepo.client.FcrepoClient;
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
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;

/**
 * @author Aaron Coburn
 * @since February 8, 2016
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
    public Option[] config() {
        final ConfigurationManager cm = new ConfigurationManager();
        final String fcrepoPort = cm.getProperty("fcrepo.dynamic.test.port");
        final String jmsPort = cm.getProperty("fcrepo.dynamic.jms.port");
        final String reindexingPort = cm.getProperty("fcrepo.dynamic.reindexing.port");
        final String rmiRegistryPort = cm.getProperty("karaf.rmiRegistry.port");
        final String rmiServerPort = cm.getProperty("karaf.rmiServer.port");
        final String sshPort = cm.getProperty("karaf.ssh.port");
        final String fcrepoBaseUrl = "localhost:" + fcrepoPort + "/fcrepo/rest";

        final String version = cm.getProperty("project.version");
        final String fcrepoAudit = getBundleUri("fcrepo-audit-triplestore", version);
        final String fcrepoFixity = getBundleUri("fcrepo-fixity", version);
        final String fcrepoReindexing = getBundleUri("fcrepo-reindexing", version);
        final String fcrepoSerialization = getBundleUri("fcrepo-serialization", version);
        final String fcrepoIndexingSolr = getBundleUri("fcrepo-indexing-solr", version);
        final String fcrepoIndexingTriplestore = getBundleUri("fcrepo-indexing-triplestore", version);
        final String fcrepoServiceAmq = getBundleUri("fcrepo-service-activemq", version);

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
                        .type("xml").classifier("features").versionAsInProject(), "camel-mustache",
                        "camel-blueprint", "camel-http4", "camel-spring", "camel-exec", "camel-jetty9",
                        "camel-jacksonxml"),
            features(maven().groupId("org.apache.activemq").artifactId("activemq-karaf")
                        .type("xml").classifier("features").versionAsInProject(), "activemq-camel"),
            features(maven().groupId("org.fcrepo.camel").artifactId("fcrepo-camel")
                        .type("xml").classifier("features").versionAsInProject(), "fcrepo-camel"),
            mavenBundle().groupId("org.codehaus.woodstox").artifactId("woodstox-core-asl").versionAsInProject(),

            CoreOptions.systemProperty("o.f.c.serialization-bundle").value(fcrepoSerialization),
            CoreOptions.systemProperty("o.f.c.fixity-bundle").value(fcrepoFixity),
            CoreOptions.systemProperty("o.f.c.reindexing-bundle").value(fcrepoReindexing),
            CoreOptions.systemProperty("o.f.c.a.triplestore-bundle").value(fcrepoAudit),
            CoreOptions.systemProperty("o.f.c.i.triplestore-bundle").value(fcrepoIndexingTriplestore),
            CoreOptions.systemProperty("o.f.c.i.solr-bundle").value(fcrepoIndexingSolr),
            CoreOptions.systemProperty("o.f.c.s.activemq-bundle").value(fcrepoServiceAmq),

            bundle(fcrepoAudit).start(),
            bundle(fcrepoIndexingSolr).start(),
            bundle(fcrepoIndexingTriplestore).start(),
            bundle(fcrepoFixity).start(),
            bundle(fcrepoSerialization).start(),
            bundle(fcrepoReindexing).start(),
            bundle(fcrepoServiceAmq).start(),

            CoreOptions.systemProperty("fcrepo.port").value(fcrepoPort),
            CoreOptions.systemProperty("karaf.reindexing.port").value(reindexingPort),
            editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiRegistryPort", rmiRegistryPort),
            editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiServerPort", rmiServerPort),
            editConfigurationFilePut("etc/org.apache.karaf.shell.cfg", "sshPort", sshPort),
            editConfigurationFilePut("etc/org.fcrepo.camel.audit.cfg", "fcrepo.baseUrl", fcrepoBaseUrl),
            editConfigurationFilePut("etc/org.fcrepo.camel.indexing.triplestore.cfg", "fcrepo.baseUrl", fcrepoBaseUrl),
            editConfigurationFilePut("etc/org.fcrepo.camel.indexing.solr.cfg", "fcrepo.baseUrl", fcrepoBaseUrl),
            editConfigurationFilePut("etc/org.fcrepo.camel.serialization.cfg", "fcrepo.baseUrl", fcrepoBaseUrl),
            editConfigurationFilePut("etc/org.fcrepo.camel.serialization.cfg", "serialization.descriptions",
                    "data/tmp/descriptions"),
            editConfigurationFilePut("etc/org.fcrepo.camel.reindexing.cfg", "fcrepo.baseUrl", fcrepoBaseUrl),
            editConfigurationFilePut("etc/org.fcrepo.camel.reindexing.cfg", "rest.port", reindexingPort),
            editConfigurationFilePut("etc/org.fcrepo.camel.service.activemq.cfg", "jms.brokerUrl",
                    "tcp://localhost:" + jmsPort)
       };
    }


    @Test
    public void testInstallation() throws Exception {

        assertTrue(featuresService.isInstalled(featuresService.getFeature("camel-core")));
        assertTrue(featuresService.isInstalled(featuresService.getFeature("fcrepo-camel")));
        assertTrue(featuresService.isInstalled(featuresService.getFeature("activemq-camel")));
        assertTrue(featuresService.isInstalled(featuresService.getFeature("camel-blueprint")));
        assertTrue(featuresService.isInstalled(featuresService.getFeature("camel-http4")));
        assertTrue(featuresService.isInstalled(featuresService.getFeature("camel-jetty9")));
        assertNotNull(bundleContext);

        assertEquals(ACTIVE, bundleContext.getBundle(System.getProperty("o.f.c.serialization-bundle")).getState());
        assertEquals(ACTIVE, bundleContext.getBundle(System.getProperty("o.f.c.fixity-bundle")).getState());
        assertEquals(ACTIVE, bundleContext.getBundle(System.getProperty("o.f.c.reindexing-bundle")).getState());
        assertEquals(ACTIVE, bundleContext.getBundle(System.getProperty("o.f.c.i.solr-bundle")).getState());
        assertEquals(ACTIVE, bundleContext.getBundle(System.getProperty("o.f.c.i.triplestore-bundle")).getState());
        assertEquals(ACTIVE, bundleContext.getBundle(System.getProperty("o.f.c.s.activemq-bundle")).getState());
    }

    @Test
    public void testReindexingService() throws Exception {
        final CamelContext ctx = getOsgiService(CamelContext.class, "(camel.context.name=FcrepoIndexer)", 10000);
        assertNotNull(ctx);

        // We aren't running solr or a triplestore, so stop these to prevent
        // unnecessary errors.
        bundleContext.getBundle(System.getProperty("o.f.c.i.solr-bundle")).stop();
        bundleContext.getBundle(System.getProperty("o.f.c.i.triplestore-bundle")).stop();

        final FcrepoClient fcrepoClient = new FcrepoClient(null, null, null, true);
        final URI baseUrl = create("http://localhost:" + System.getProperty("fcrepo.port") + "/fcrepo/rest");
        final URI url1 = fcrepoClient.post(baseUrl, null, null).getLocation();
        final URI url2 = fcrepoClient.post(baseUrl, null, null).getLocation();
        fcrepoClient.post(url1, null, null);
        fcrepoClient.post(url2, null, null);

        final MockEndpoint resultEndpoint = (MockEndpoint) ctx.getEndpoint("mock:results");
        resultEndpoint.expectedMessageCount(5);

        final CloseableHttpClient client = createDefault();
        final String reindexingUrl = "http://localhost:" + System.getProperty("karaf.reindexing.port") + "/reindexing/";
        try (final CloseableHttpResponse response = client.execute(new HttpGet(reindexingUrl))) {
            assertEquals(SC_OK, response.getStatusLine().getStatusCode());
        }

        final HttpPost post = new HttpPost(reindexingUrl);
        post.addHeader("Content-Type", "application/json");
        post.setEntity(new StringEntity("[\"mock:results\"]"));
        try (final CloseableHttpResponse response = client.execute(post)) {
            assertEquals(SC_OK, response.getStatusLine().getStatusCode());
        }

        assertIsSatisfied(resultEndpoint);
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

    private static String getBundleUri(final String artifactId, final String version) {
        final File artifact = new File(getBaseDir() + "/../" + artifactId + "/target/" +
                artifactId + "-" + version + ".jar");
        if (artifact.exists()) {
            return artifact.toURI().toString();
        }
        return "mvn:org.fcrepo.camel/" + artifactId + "/" + version;
    }

}
