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

* at: Audit Service (Triplestore)
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
[fcrepo-audit](http://github.com/fcrepo4-exts/fcrepo-audit)).  Events about
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

    triplestore.baseUrl=localhost:8080/fuseki/test/update

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

    triplestore.baseUrl=localhost:8080/fuseki/test/update

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
such as fcrepo-indexing-solr, fcrepo-indexing-triplestore or fcrepo-fixity.

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

##Fedora Fixity Checking Service

This application implements a fixity checking service that can be used
in conjunction with the reindexing service. Each `Binary` resource
encountered will have its checksum (fixity) value checked. If the result
is `SUCCESS`, the full response will be sent to the `fixity.success` endpoint.
Otherwise, the response will be sent to `fixity.failure`.

###Configuration

A number of application values can be configured externally, through
system properties. These include:

The log directory for fixity errors:

    fcrepo.fixity.logdir=/tmp

The fixity error log:

    fcrepo.fixity.logfile=fixityErrors.log

Alternately, the application can be configured by updating the `application.properties`
configuration file in the unpacked `WEB-INF/classes/application.properties` file.

As with other components, `error.maxRedeliveries`, `jms.brokerUrl` and the `fcrepo.*`
configurations can be changed to match local settings. The configurations specific
to this service are listed here:

The camel URI for the incoming message stream.

    fixity.stream=activemq:queue:fixity

Because fixity checking can put a significant load on a server, it can be convenient
to introduce a delay between each fixity check. That delay is measured in milliseconds.

    fixity.delay=0

Most importantly, it is possible to configure what should happen when a fixity check fails.
By default, the fixity output is written to a file in `/tmp/fixityErrors.log`. But this can
be changed to send a message to an email address (`fixity.failure=smtp:admin@example.org?subject=Fixity`)
or use just about any other camel component.

    fixity.failure=file:/tmp/?fileName=fixityErrors.log&fileExist=Append

It is also possible to trigger an action on success (by default, this is a no-op):

    fixity.success=mock:fixity.success

##Further Information
For more help see the Apache Camel documentation

    http://camel.apache.org/

