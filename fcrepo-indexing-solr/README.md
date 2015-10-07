#Fedora Indexing Service (Solr)

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

##Building

To build this project use

    MAVEN_OPTS="-Xmx1024m" mvn install

##Running from the command line

To run the project you can execute the following Maven goal

    mvn camel:run

##Deploying in OSGi

This project can be deployed in an OSGi container. For example using
[Apache ServiceMix](http://servicemix.apache.org/) or 
[Apache Karaf](http://karaf.apache.org), you can run the following
command from its shell:

    feature:repo-add mvn:org.fcrepo.camel/fcrepo-camel-toolbox/LATEST/xml/features
    feature:install fcrepo-indexing-solr

##Deploying in Tomcat/Jetty

If you intend to deploy this application in a web container such as Tomcat or Jetty,
please refer to the documentation in the
[fcrepo-camel-webapp](https://github.com/fcrepo4-labs/fcrepo-camel-webapp)
project.

##Configuration

The application can be configured by creating a file in
`$KARAF_HOME/etc/org.fcrepo.camel.indexing.solr.cfg`. The following
values are available for configuration:

In the event of failure, the maximum number of times a redelivery will be attempted.

    error.maxRedeliveries=10

If the fedora repository requires authentication, the following values
can be set:

    fcrepo.authUsername=<username>
    fcrepo.authPassword=<password>
    fcrepo.authHost=<host realm>

The baseUrl for the fedora repository.

    fcrepo.baseUrl=localhost:8080/fcrepo/rest

The default `LDPath` transformation to use. This is overridden on a per-object
basis with the `indexing:hasIndexingTransformation` predicate.

    fcrepo.defaultTransform=default

If you would like to index only those objects with a type `indexing:Indexable`,
set this property to `true`

    indexing.predicate=false

The JMS connection URI, used for connecting to a local or remote ActiveMQ broker.

    jms.brokerUrl=tcp://localhost:61616

The camel URI for the incoming message stream.

    input.stream=activemq:topic:fedora

The camel URI for handling reindexing events.

    solr.reindex.stream=activemq:queue:solr.reindex

The baseUrl for the Solr server. If using Solr 4.x or better, the URL should include
the core name.

    solr.baseUrl=localhost:8983/solr/collection1

The timeframe (in milliseconds) within which new items should be committed to the solr index.

    solr.commitWithin=10000

The location of the internal Audit trail, if using the `fcrepo-audit` extension module.
Nodes at this location will not be indexed.

    audit.container=/audit

By editing this file, any currently running routes will be immediately redeployed
with the new values.

For more help see the Apache Camel documentation

    http://camel.apache.org/

