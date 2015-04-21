# Fedora Messaging Application Toolbox

A collection of ready-to-use messaging applications for use
with [Fedora4](http://fcrepo.org). These applications use
[Apache Camel](https://camel.apache.org).

[![Build Status](https://travis-ci.org/fcrepo4-labs/fcrepo-camel-toolbox.png?branch=master)](https://travis-ci.org/fcrepo4-labs/fcrepo-camel-toolkit)

## Applications

### Repository Audit Service (Triplestore)

This application listens to Fedora's event stream, and stores
audit-related events in an external triplestore. Both
[Jena Fuseki](http://jena.apache.org/documentation/serving_data/)
and [Open RDF Sesame](http://rdf4j.org/) are supported.

More information about the proposed
[audit service](https://wiki.duraspace.org/display/FF/Design+-+Audit+Service)
is available on the Fedora wiki.

This project is available as both an OSGi bundle (`audit-triplestore`)
and as a web-deployable `war` artifact (`audit-triplestore-web`),
so it can be deployed in either Karaf or a web container such as
Jetty/Tomcat.

### Repository Indexer (Solr)

This application listens to Fedora's event stream and
indexes objects into an external Solr server.

### Repository Indexer (Triplestore)

This application listens to Fedora's event stream and
indexes objects into an external triplestore.

## Building

To build these projects use this command

    MAVEN_OPTS="-Xmx1024m" mvn clean install
