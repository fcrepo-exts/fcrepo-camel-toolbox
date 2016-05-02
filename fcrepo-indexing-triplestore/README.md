#Fedora Indexing Service (Triplestore)

This application implements a bridge to an external triplestore,
such as Sesame or Fuseki for [Fedora4](http://fcrepo.org).

Additional background information on this service is available on the Fedora Wiki on the
[Integration Services page](https://wiki.duraspace.org/display/FEDORA4x/Integration+Services).

##Building

To build this project use

    MAVEN_OPTS="-Xmx1024m" mvn install

##Deploying in OSGi

This project can be deployed in an OSGi container. For example using
[Apache ServiceMix](http://servicemix.apache.org/) or 
[Apache Karaf](http://karaf.apache.org), you can run the following
command from its shell:

    feature:repo-add mvn:org.fcrepo.camel/toolbox-features/LATEST/xml/features
    feature:install fcrepo-indexing-triplestore

##Configuration

The application can be configured by creating a file in
`$KARAF_HOME/etc/org.fcrepo.camel.indexing.triplestore.cfg`. The following
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

If you would like to index only those objects with a type `indexing:Indexable`,
set this property to `true`

    indexing.predicate=false

It is possible to control the representation of fedora resources with Prefer headers
by including or excluding certain types of triples. For instance, `ldp:contains` triples
are excluded by default. This is so because, for large repositories, the `ldp:contains` triples
may number in the hundreds of thousands or millions of triples, which lead to very large
request/response sizes. It is important to note that `fedora:hasParent` functions as a logical
inverse of `ldp:contains`, so in the context of a triplestore, you can use the inverse
property in SPARQL queries to much the same effect. Alternately, a built-in reasoner will
allow you to work directly with `ldp:contains` triples even if they haven't been explicitly
added to the triplestore.

    prefer.omit=http://www.w3.org/ns/ldp#PreferContainment
    prefer.include=

The JMS connection URI, used for connecting to a local or remote ActiveMQ broker.

    jms.brokerUrl=tcp://localhost:61616

If the JMS connection requires authentication, these parameters should be populated

    jms.username=<username>
    jms.password=<password>

The camel URI for the incoming message stream.

    input.stream=activemq:topic:fedora

The camel URI for handling reindexing events.

    triplestore.reindex.stream=activemq:queue:triplestore.reindex

The base URL of the triplestore being used.

    triplestore.baseUrl=localhost:8080/fuseki/test/update

A named graph for any objects being indexed in the triplestore. This value, if
not left blank, should be a valid URI.

    triplestore.namedGraph=

The location of the internal Audit trail, if using the `fcrepo-audit` extension module.
Nodes at this location will not be indexed.

    audit.container=/audit

By editing this file, any currently running routes will be immediately redeployed
with the new values.

For more help see the Apache Camel documentation

    http://camel.apache.org/

