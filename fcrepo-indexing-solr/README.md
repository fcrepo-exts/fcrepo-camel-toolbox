# Fedora Indexing Service (Solr)

This application implements a bridge to an external, solr index
for [Fedora4](http://fcrepo.org).

The application relies on LDPath-based transformations to convert a resource
from RDF into JSON. More information on the LDPath language is available on the
[Marmotta website](http://marmotta.apache.org/ldpath/language.html). It may also
be helpful to read about Fedora's
[transformation API](https://wiki.duraspace.org/display/FEDORA4x/RESTful+HTTP+API+-+Transform),
which describes how to install special purpose LDPath programs in Fedora.

Additional background information is available on the Fedora Wiki on the
[Integration Services page](https://wiki.duraspace.org/display/FEDORA4x/Integration+Services).

Please also note that in order to push data into Solr, your Solr schema must
be configured to accept the data you intend to send. This is typically accomplished
by updating Solr's `[schema.xml](https://wiki.apache.org/solr/SchemaXml)` file,
found in `$SOLR_HOME/<core name>/conf/` (for Solr 4.x). Starting with 5.2, Solr provides a
[schema API](https://cwiki.apache.org/confluence/display/solr/Schema+API),
which allows runtime (re-)configuration of Solr. Further information on using Solr
can be found at the [Solr website](http://lucene.apache.org/solr/)

## Building

To build this project use

    MAVEN_OPTS="-Xmx1024m" mvn install

## Deploying in OSGi

This project can be deployed in an OSGi container. For example using
[Apache ServiceMix](http://servicemix.apache.org/) or 
[Apache Karaf](http://karaf.apache.org), you can run the following
command from its shell:

    feature:repo-add mvn:org.fcrepo.camel/toolbox-features/LATEST/xml/features
    feature:install fcrepo-indexing-solr
    feature:install fcrepo-service-activemq

## Configuration

The application can be configured by creating a file in
`$KARAF_HOME/etc/org.fcrepo.camel.indexing.solr.cfg`. The following
values are available for configuration:

In the event of failure, the maximum number of times a redelivery will be attempted.

    error.maxRedeliveries=10

If you would like the `indexing:hasIndexingTransformation` property to be checked
on a per-object basis, set this to true. Otherwise, the `fcrepo.defaultTransform`
is always used as transformation URL even if an object has the
`indexing:hasIndexingTransformation` property set.

    fcrepo.checkHasIndexingTransformation=true

The default `LDPath` transformation to use. This is overridden on a per-object
basis with the `indexing:hasIndexingTransformation` predicate. The default value is empty,
but it may be overridden with a public URL as shown below.

    fcrepo.defaultTransform=http://example.com/ldpath/program.txt

The location of the LDPath service.

    ldpath.service.baseUrl=http://localhost:9086/ldpath

If you would like to index only those objects with a type `indexing:Indexable`,
set this property to `true`

    indexing.predicate=false

The camel URI for the incoming message stream.

    input.stream=broker:topic:fedora

The camel URI for handling reindexing events.

    solr.reindex.stream=broker:queue:solr.reindex

The baseUrl for the Solr server. If using Solr 4.x or better, the URL should include
the core name. (Use `https` scheme if your solr server requires https and/or ssl client certificate authentication.)

    solr.baseUrl=http://localhost:8983/solr/collection1


The timeframe (in milliseconds) within which new items should be committed to the solr index.

    solr.commitWithin=10000

A comma-delimited list of URIs to filter. That is, any Fedora resource that either
matches or is contained in one of the URIs listed will not be processed by the
fcrepo-indexing-solr application.

    filter.containers=http://localhost:8080/fcrepo/rest/audit

By editing this file, any currently running routes will be immediately redeployed
with the new values.

For more help see the Apache Camel documentation

    http://camel.apache.org/

