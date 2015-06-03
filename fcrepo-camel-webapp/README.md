#Web-Deployable Fedora/Camel integration components

##Building

This project can be built with all of the services enabled or with only
selected services enabled.

To build this with all services enabled, use

    MAVEN_OPTS="-Xmx1024m" mvn install -Pis -Pit -Pat -Prs

Note: The following syntax is also valid: `mvn install -Pis,it,at,rs`

To build only the audit service, use

    MAVEN_OPTS="-Xmx1024m" mvn install -Pat

Or, to build only the Solr indexing and reindexing service, use

    MAVEN_OPTS="-Xmx1024m" mvn install -Pis,rs

Any combination of services is possible, where the profile codes the following meanings:

* at: Audit Service
* is: Indexing Service (Solr)
* it: Indexing Service (Triplestore)
* rs: Reindexing Service

##Fedora Audit Service (Triplestore)

This application implements a bridge to an external, triplestore-based
[Audit Service](https://wiki.duraspace.org/display/FF/Design+-+Audit+Service)
for [Fedora4](http://fcrepo.org).

###Configuration

The application can be configured by updating the following configuration
file `src/main/resources/application.properties`. The following values
are available for configuration:

The name of the container where internal audit events are created (if using
[fcrepo-audit](http://github.com/fcrepo4-labs/fcrepo-audit)).  Events about
resources in this container are ignored.

    audit.container=/audit

In the event of failure, the maximum number of times a redelivery will be attempted.

    error.maxRedeliveries=10

The baseUri to use for event URIs in the triplestore. A `UUID` will be appended
to this value, forming, for instance: `http://example.com/event/{UUID}`

    event.baseUri=http://example.com/event

The connection URI used to connect to a local or remote ActiveMQ broker

    jms.brokerUrl=tcp://localhost:61616

The camel URI for the incoming message stream.

        input.stream=activemq:topic:fedora

The base URL of the triplestore being used.

    triplestore.baseUrl=localhost:3030/test/update

##Fedora Indexing Service (Triplestore)

This application implements a bridge to an external triplestore,
such as Sesame or Fuseki
for [Fedora4](http://fcrepo.org).

###Configuration

The application can be configured by updating the following configuration
file `src/main/resources/application.properties`. The following values
are available for configuration:

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

The JMS connection URI, used for connecting to a local or remote ActiveMQ broker.

    jms.brokerUrl=tcp://localhost:61616

The camel URI for the incoming message stream.

    input.stream=activemq:topic:fedora

The base URL of the triplestore being used.

    triplestore.baseUrl=localhost:3030/test/update

A named graph for any objects being indexed in the triplestore. This value, if
not left blank, should be a valid URI.

    triplestore.namedGraph=

##Fedora Indexing Service (Solr)

This application implements a bridge to an external, solr index
for [Fedora4](http://fcrepo.org).

###Configuration

The application can be configured by updating the following configuration
file `src/main/resources/application.properties`. The following values
are available for configuration:

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

The camel URI for a reindexing queue.

    reindexing.stream=activemq:queue:reindexing

The baseUrl for the Solr server. If using Solr 4.x or better, the URL should include
the core name.

    solr.baseUrl=localhost:8983/solr/collection1

The timeframe (in milliseconds) within which new items should be committed to the solr index.

    solr.commitWithin=10000

##Fedora Reindexing Service

This application implements a reindexing service for other components,
such as fcrepo-indexing-solr or fcrepo-indexing-triplestore.

###Configuration

A number of application values can be configured externally, through
system properties. These include:

The prefix for the exposed REST endpoint

    fcrepo.reindexing.prefix=/reindexing

The port used for the REST endpoint

    fcrepo.dynamic.reindexing.port=9080

Alternately, the application can be configured by updating the `application.properties`
configuration file in the unpacked `WEB-INF/classes/application.properties` file.
The following values are available for configuration:

In the event of failure, the maximum number of times a redelivery will be attempted.

    error.maxRedeliveries=10

If the fedora repository requires authentication, the following values
can be set:

    fcrepo.authUsername=<username>
    fcrepo.authPassword=<password>
    fcrepo.authHost=<host realm>

The baseUrl for the fedora repository.

    fcrepo.baseUrl=localhost:8080/fcrepo/rest

The JMS connection URI, used for connecting to a local or remote ActiveMQ broker.

    jms.brokerUrl=tcp://localhost:61616

The camel URI for the internal processing queue.

    input.stream=activemq:queue:reindexing

The prefix for the REST endpoint.

    rest.prefix=/reindexing

The port for the REST endpoint.

    rest.port=9080

##Further Information
For more help see the Apache Camel documentation

    http://camel.apache.org/

