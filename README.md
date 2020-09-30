# Fedora Messaging Application Toolbox

A collection of ready-to-use messaging applications for use
with [Fedora4](http://fcrepo.org). These applications use
[Apache Camel](https://camel.apache.org).

[![Build Status](https://travis-ci.org/fcrepo-exts/fcrepo-camel-toolbox.png?branch=master)](https://travis-ci.org/fcrepo-exts/fcrepo-camel-toolbox)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.fcrepo.camel/toolbox-features/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.fcrepo.camel/toolbox-features/)

Additional background information is available on the Fedora Wiki on the
[Integration Services page](https://wiki.duraspace.org/display/FEDORA4x/Integration+Services).

## Applications

Each of these applications are available as OSGi bundles and can be deployed
directly into an OSGi container such as Karaf. 

## Note

Please note: the RDF representation of Fedora Resources is sensitive to the `Host` header
supplied by any client. This can lead to potentially surprising effects from the perspective
of the applications in this Messaging Toolbox.

For example, if the `fcrepo-indexing-triplestore` connects to Fedora at `http://localhost:8080`
but another client modifies Fedora resources at `http://repository.example.edu`, you may
end up with incorrect and/or duplicate data in downstream applications. It is far better to
force clients to connect to Fedora over a non-`localhost` network interface.
Depending on your deployment needs, you may also consider setting a static `Host` header in a proxy.
For instance, with `nginx`, to proxy Fedora over a standard web port, this configuration may suffice:
```
    location /fcrepo {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Host repository.example.edu;
    }
```
Any such reverse proxy will work. Then, if port 8080 is inaccessible from outside the
deployment server, and all clients (including these messaging toolbox applications) access Fedora
with the `baseUrl` set to something like: `http://repository.example.edu/fcrepo/rest`,
then the asynchonous integrations will be less prone to configuration errors.

### Repository Audit Service (Triplestore)

This application listens to Fedora's event stream, and stores
audit-related events in an external triplestore. Both
[Jena Fuseki](http://jena.apache.org/documentation/serving_data/)
and [Open RDF Sesame](http://rdf4j.org/) are supported.

More information about the
[audit service](https://wiki.duraspace.org/display/FF/Design+-+Audit+Service)
is available on the Fedora wiki.

### Repository Indexer (Solr)

This application listens to Fedora's event stream and
indexes objects into an external Solr server.

### Repository Indexer (Triplestore)

This application listens to Fedora's event stream and
indexes objects into an external triplestore.

### Fixity Checking Service

This application can be used in conjunction with the Repository
Re-Indexer to verify the checksums for all Binary resources in
the repository.

### Repository Serializer

This application automatically serializes Fedora resources to a filesystem
location. Using the re-indexer, one can optionally serialize particular
segments of the repository to a location on the filesystem.

### Repository Re-Indexer

This application allows a user to initiate a re-indexing process
from any location within the Fedora node hierarchy, sending
re-indexing requests to a specified list of external applications
(e.g. fcrepo-indexing-solr, fcrepo-indexing-triplestore, and/or
fcrepo-fixity).

One can specify which applications/endpoints to send these 
reindexing events, by POSTing a JSON array to the re-indexing
service:

    curl -XPOST localhost:9080/reindexing/fedora/path -H"Content-Type: application/json" \
        -d '["broker:queue:solr.reindex","broker:queue:fixity","broker:queue:triplestore.reindex"]'

## Building

To build these projects use this command

    MAVEN_OPTS="-Xmx1024m" mvn clean install

## OSGi deployment (Karaf 4.x)

These applications are distributed as OSGi features, making it easy to deploy these
applications in a Karaf container. There are several ways to install these features, and
it may be useful to refer to the Karaf documentation related to
[provisioning](https://karaf.apache.org/manual/latest/#_provisioning).

### Production deployment

For production use, it is recommended to make use of Karaf's [boot features](https://karaf.apache.org/manual/latest/#_boot_features).
This involves editing the `$KARAF_HOME/etc/org.apache.karaf.features.cfg` configuration file. The two relevant
configuration options are:

  * `featuresRepositories`, which contains a comma-separated list of features repository URLs.
  * `featuresBoot`, which contains a comma-separated list of feature names.

To install `fcrepo-camel-toolbox/4.6.2`, one would add the following:

      featuresRepositories = \
          ..., \
          mvn:org.fcrepo.camel/toolbox-features/4.6.2/xml/features

To install version 4.7.1 of the `fcrepo-camel-toolbox`, one will also need to specify the version of Camel and ActiveMQ like so:

      featuresRepositories = \
          ..., \
          mvn:org.apache.activemq/activemq-karaf/5.14.0/xml/features, \
          mvn:org.apache.camel.karaf/apache-camel/2.18.0/xml/features, \
          mvn:org.fcrepo.camel/toolbox-features/4.7.1/xml/features

Users are not resticted to particular versions of Camel and ActiveMQ, so long as Camel is
at least version 2.18.0 and ActiveMQ is at least version 5.14.0.

In order to add particular features into a Karaf container at boot time, the `featuresBoot`
configuration value should be edited to include the desired features. For instance, to install
`fcrepo-indexing-triplestore`, one would add the following to `featuresBoot`:

    featuresBoot = \
        ..., \
        fcrepo-service-activemq, \
        fcrepo-service-camel, \
        fcrepo-indexing-triplestore

For most features, it is necessary to explicitly specify the `fcrepo-service-activemq` feature.

With this configuration in place, it is possible to upgrade the version of `fcrepo-camel-toolbox` by
simply shutting down Karaf, deleting the `$KARAF_HOME/data` directory, updating the version number(s)
in the `featuresRepositories` configuration and restarting Karaf. The updated features
will automatically re-deploy in a fresh Karaf environment.

**Note**: When installing a fcrepo-camel-toolbox feature repository, it is recommended to use a released version
rather than specifying `LATEST`. The latest released version can be found by inspecting the Maven Central badge at
top of this README file.

### Deployment for development or testing

When testing karaf features, it can be more convenient to install/uninstall them directly from the Karaf console.

To do this, first, add the `toolbox-features` repository:

    $> feature:repo-add mvn:org.fcrepo.camel/toolbox-features/4.6.2/xml/features

Or, if you are using version 4.7.1 or later, also add the Camel and ActiveMQ repositories:

    $> feature:repo-add camel 2.18.0
    $> feature:repo-add activemq 5.14.0
    $> feature:repo-add mvn:org.fcrepo.camel/toolbox-features/4.7.1/xml/features

Then, you can add any combination of the following applications:

    $> feature:install fcrepo-service-activemq
    $> feature:install fcrepo-audit-triplestore
    $> feature:install fcrepo-fixity
    $> feature:install fcrepo-indexing-solr
    $> feature:install fcrepo-indexing-triplestore
    $> feature:install fcrepo-ldpath
    $> feature:install fcrepo-reindexing
    $> feature:install fcrepo-serialization
    $> feature:install fcrepo-service-ldcache-file

## Maintainers

Current maintainers:

* [Peter Eichman](https://github.com/peichman-umd)
* [Daniel Lamb](https://github.com/dannylamb)
* [Bethany Seeger](https://github.com/bseeger)
