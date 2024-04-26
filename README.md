# Fedora Messaging Application Toolbox

A collection of ready-to-use messaging applications for use
with [Fedora](http://fcrepo.org). These applications use
[Apache Camel](https://camel.apache.org).

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.fcrepo.camel/toolbox-features/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.fcrepo.camel/toolbox-features/)

Additional background information is available on the Fedora Wiki on the
[Integration Services page](https://wiki.duraspace.org/display/FEDORA4x/Integration+Services).

## Prerequisites
* Java 11
* Solr
* Fedora 6

NOTE:  This is project is currently in a state of flux as we are in the process of upgrading it to support Java 11 and Camel 3.9.x
Currently the Solr, ActiveMQ, Reindexing, and LDPath microservices are available. Triplestore indexing and Fixity are coming soon.

## Building

To build these projects use this command

    MAVEN_OPTS="-Xmx1024m" mvn clean install


## Running the toolbox

The camel toolbox can be run as a java cli application. Once the maven build is done, run the toolbox driver:
```
java -jar fcrepo-camel-toolbox-app/target/fcrepo-camel-toolbox-app-<version>-driver.jar -c configuration.properties
```

where your `configuration.properties` file is a standard java properties file.

### Docker Compose

The Camel Toolbox can be started using Docker Compose which will create containers for Fedora, Fuseki, Solr, and the
Camel Toolbox application. 

Configuration for the Camel Toolbox can be done through the `docker-compose/camel-toolbox-config/configuration.properties` or through 
environment variables (not yet available) as standard java properties as key value pairs. To run with the docker containers 
the following properties are set by default:
```
jms.brokerUrl=tcp://fcrepo:61616
fcrepo.baseUrl=http://fcrepo:8080/fcrepo/rest

solr.indexing.enabled=true
solr.baseUrl=http://solr:8983/solr/fcrepo

triplestore.indexing.enabled=true
triplestore.baseUrl=http://fuseki:3030/fcrepo

audit.enabled=true
fixity.enabled=true
fcrepo.authHost=fcrepo
reindexing.rest.host=0.0.0.0
```

Then to start,  the Camel Toolbox, Fedora, Fuseki, and Solr containers run
```
cd ./docker-compose
docker-compose up -d
```

If you need to rebuild the docker image locally you can do so like so:
```
mvn clean install
FCREPO_CAMEL_TOOLBOX_VERSION=$(mvn org.apache.maven.plugins:maven-help-plugin:3.2.0:evaluate -Dexpression=project.version -q -DforceStdout)
docker buildx build --load --tag="fcrepo/fcrepo-camel-toolbox" --tag="fcrepo/fcrepo-camel-toolbox:${FCREPO_CAMEL_TOOLBOX_VERSION}" .
```

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

### Global Properties

| Name      | Description| Default Value |
| :---      | :---| :----   |
| fcrepo.baseUrl | The base url endpoint for your Fedora installation.  | http://localhost:8080/fcrepo/rest |
| fcrepo.authUsername | A valid username      | fcrepoAdmin |
| fcrepo.authPassword | A valid password      | fcrepoAdmin |
| fcrepo.authHost | The hostname of the Fedora installation which the fcrepo.authUsername and fcrepo.authPassword should be applied to      | localhost |
| error.maxRedeliveries | The maximum number of redelivery attempts before failing.      | 10 |

### ActiveMQ Service

This implements a connector to an ActiveMQ broker.

#### Properties
| Name      | Description| Default Value |
| :---      | :---| :----   |
| jms.brokerUrl | JMS Broker endpoint | tcp://localhost:61616 |
| jms.username | JMS username | null |
| jms.password | JMS password | null |
| jms.connections | The JMS connection count | 10 |
| jms.consumers | The JMS consumer count | 1 |


### Repository Indexer (Solr)

This application listens to Fedora's event stream and
indexes objects into an external Solr server.

#### Properties
| Name      | Description                                                                                                                                                 | Default Value |
| :---      |:------------------------------------------------------------------------------------------------------------------------------------------------------------| :----   |
| solr.indexing.enabled | Enables/disables the SOLR indexing service. Disabled by default                                                                                             | false | 
| solr.fcrepo.checkHasIndexingTransformation | When true, check for an indexing transform in the resource metadata with the predicate http://fedora.info/definitions/v4/indexing#hasIndexingTransformation | true |
| solr.fcrepo.defaultTransform | The solr default XSL transform when none is provide in resource metadata.                                                                                   | null | 
| solr.input.stream | The JMS topic or queue serving as the message source                                                                                                        | broker:topic:fedora |
| solr.reindex.stream | The JMS topic or queue serving as the reindex message source                                                                                                | broker:queue:solr.reindex |
| solr.commitWithin | Milliseconds within which commits should occur                                                                                                              | 10000 |
| solr.indexing.predicate | When true, check that resource is of type http://fedora.info/definitions/v4/indexing#Indexable; otherwise do not index it.                                  | false |
| solr.filter.containers | A comma-separate list of containers that should be ignored by the indexer                                                                                   | http://localhost:8080/fcrepo/rest/audit |

**Note**: You must start with the `file://` protocol when defining the path to a custom XSLT for either the `solr.fcrepo.defaultTransform` 
or within the resource using the `http://fedora.info/definitions/v4/indexing#hasIndexingTransformation` predicate. 

For example, 
```text
solr.fcrepo.defaultTransform=file:///path/to/your/transform.xsl
```
or
```text
@prefix indexing: <http://fedora.info/definitions/v4/indexing#> .
<> indexing:hasIndexingTransformation <file:///path/to/your/transform.xsl> .
```

### Repository Indexer (Triplestore)

This application listens to Fedora's event stream and
indexes objects into an external triplestore.

#### Properties
| Name      | Description| Default Value |
| :---      | :---| :----   |
| triplestore.indexing.enabled | Enables the triplestore indexing service. Disabled by default | false | 
| triplestore.baseUrl | Base URL for the triplestore | http://localhost:8080/fuseki/test/update | 
| triplestore.authUsername | Username for basic authentication against triplestore | 
| triplestore.authPassword | Password for basic authentication against triplestore | 
| triplestore.input.stream |   The JMS topic or queue serving as the message source    | broker:topic:fedora | 
| triplestore.reindex.stream |   The JMS topic or queue serving as the reindex message source    | broker:queue:triplestore.reindex | 
| triplestore.indexing.predicate | When true, check that resource is of type http://fedora.info/definitions/v4/indexing#Indexable; otherwise do not index it.   | false |
| triplestore.filter.containers |   A comma-separate list of containers that should be ignored by the indexer  | http://localhost:8080/fcrepo/rest/audit | 
| triplestore.namedGraph |  A named graph to be used when indexing rdf  | null |  
| triplestore.prefer.include |  A list of [valid prefer values](https://fedora.info/2021/05/01/spec/#additional-prefer-values) defining predicates to be included  | null |  
| triplestore.prefer.omit | A list of [valid prefer values](https://fedora.info/2021/05/01/spec/#additional-prefer-values) defining predicates to be omitted. | http://www.w3.org/ns/ldp#PreferContainment |  

### Reindexing Service

This application implements a reindexing service so that
any node hierarchy in fedora (e.g. the entire repository
or some subset thereof) can be reindexed by a set of external
services.

One can specify which applications/endpoints to send these 
reindexing events, by POSTing a JSON array to the re-indexing
service:

    curl -XPOST localhost:9080/reindexing/fedora/path -H"Content-Type: application/json" \
        -d '["broker:queue:solr.reindex","broker:queue:fixity","broker:queue:triplestore.reindex"]'

#### Properties
| Name      | Description| Default Value |
| :---      | :---| :----   |
| reindexing.enabled | Enables/disables the reindexing component. Enabled by default | true | 
| reindexing.stream | Reindexing jms message stream | broker:queue:reindexing | 
| reindexing.rest.host | Reindexing service host | localhost | 
| reindexing.rest.port | Reindexing service port | 9080 |
| reindexing.rest.prefix | Reindexing rest URI prefix | /reindexing | 

### HTTP Message Forwarding Service (HTTP)

This application listens to Fedora's event stream and
forwards message identifiers and event types as JSON POSTs to an HTTP endpoint.

#### Properties

| Name      | Description| Default Value |
| :---      | :---| :----   |
| http.enabled | Enables/disables the HTTP forwarding service. Disabled by default | false | 
| http.input.stream | The JMS topic or queue serving as the message source    | broker:topic:fedora |
| http.reindex.stream | The JMS topic or queue serving as the reindex message source    | broker:queue:http.reindex |
| http.filter.containers | A comma-separate list of containers that should be ignored by the indexer  | http://localhost:8080/fcrepo/rest/audit |
| http.baseUrl | The HTTP endpoint that will receive forwarded JMS messages (REQUIRED) | |
| http.authUsername | Optional username for basic authentication if required by http.baseUrl | |
| http.authPassword | Optional password for basic authentication if required by http.baseUrl | |

Note: you MUST set the http.baseUrl property in order for this service to do anything meaningful.

### Fixity Checking Service

This application can be used in conjunction with the Repository
Re-Indexer to verify the checksums for all Binary resources in
the repository.

#### Properties
| Name      | Description| Default Value |
| :---      | :---| :----   |
| fixity.enabled | Enables/disables fixity service  | false |
| fixity.input.stream | Fixity Service jms  message stream | broker:queue:fixity |
| fixity.delay | A delay in milliseconds between each fixity check to reduce load on server | 0 |
| fixity.success|  It is also possible to trigger an action on success. By default, this is a no-op. The value should be a camel route action.  To log it to a file use something like this:  file:/tmp/?fileName=fixity-succes.log&fileExist=Append | null |
| fixity.failure |  Most importantly, it is possible to configure what should happen when a fixity check fails. In the default example below, the fixity output is written to a file in `/tmp/fixityErrors.log`. But this can be changed to send a message to an email address (`fixity.failure=smtp:admin@example.org?subject=Fixity`) or use just about any other camel component.| file:/tmp/?fileName=fixity-errors.log&fileExist=Append |


### Repository Audit Service (Triplestore)

This application listens to Fedora's event stream, and stores
audit-related events in an external triplestore. Both
[Jena Fuseki](http://jena.apache.org/documentation/serving_data/)
and [Open RDF Sesame](http://rdf4j.org/) are supported.

More information about the
[audit service](https://wiki.duraspace.org/display/FF/Design+-+Audit+Service)
is available on the Fedora wiki.

#### Properties
| Name      | Description| Default Value |
| :---      | :---| :----   |
| audit.enabled | Enables/disables audit triplestore service  | false |
| audit.input.stream | Audit Service jms message stream | broker:topic:fedora |
| audit.event.baseUri | The baseUri to use for event URIs in the triplestore. A `UUID` will be appended to this value, forming, for instance: `http://example.com/event/{UUID}` | http://example.com/event |
| audit.triplestore.baseUrl| The base url for the external triplestore service | http://localhost:3030/fuseki/test/update |
| audit.triplestore.authUsername| Username for basic authentication against triplestore | |
| audit.triplestore.authPassword| Password for basic authentication against triplestore | |
| audit.filter.containers |  A comma-delimited list of URIs to be filtered (ignored) by the audit service | http://localhost:8080/fcrepo/rest/audit | 


## Troubleshooting 

### java.lang.IllegalArgumentException: Credentials may not be null

Check the `configuration.properties` to ensure that the `fcrepo.baseUrl` and `fcrepo.authHost` have
the same hostname. If they differ, the http client will not be able to find the credentials passed in
for authentication.

## Maintainers

Current maintainers:

* [Danny Bernstein](https://github.com/dbernstein)
