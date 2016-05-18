# Fedora Messaging Application Toolbox

A collection of ready-to-use messaging applications for use
with [Fedora4](http://fcrepo.org). These applications use
[Apache Camel](https://camel.apache.org).

[![Build Status](https://travis-ci.org/fcrepo4-exts/fcrepo-camel-toolbox.png?branch=master)](https://travis-ci.org/fcrepo4-exts/fcrepo-camel-toolbox)

Additional background information is available on the Fedora Wiki on the
[Integration Services page](https://wiki.duraspace.org/display/FEDORA4x/Integration+Services).

## Applications

Each of these applications are available as OSGi bundles and can be deployed
directly into an OSGi container such as Karaf. 

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

These applications are distributed as OSGi features, meaning they can be installed
directly from the karaf console. First, add the `toolbox-features` repository:

    $> feature:repo-add mvn:org.fcrepo.camel/toolbox-features/LATEST/xml/features

Then, you can add any combination of the following applications:

    $> feature:install fcrepo-indexing-solr
    $> feature:install fcrepo-indexing-triplestore
    $> feature:install fcrepo-audit-triplestore
    $> feature:install fcrepo-reindexing
    $> feature:install fcrepo-fixity
    $> feature:install fcrepo-serialization
    $> feature:install fcrepo-service-activemq


##Maintainers

Current maintainers:

* [Aaron Coburn](https://github.com/acoburn)
* [Daniel Lamb](https://github.com/daniel-dgi)

