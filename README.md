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


## Running the toolbox

Before starting the camel toolbox fire up a Fedora 6.x instance, Solr 8.x, and Fuseki (triple store).

Fedora
```
docker run -p8080:8080 --rm -p61616:61616  -p8181:8181 --name=my_fcrepo6  fcrepo/fcrepo:6.0.0
```

Solr
```
docker run  --rm -p 8983:8983 --name my_solr solr:8
```

Create the default Solr Core
```
docker exec -it my_solr solr create_core -c collection1
```

Fuseki 
```
docker run --rm -p 3030:3030 --name my_fuseki atomgraph/fuseki --mem /test
```


```
mvn clean install
java -jar fcrepo-camel-toolbox/fcrepo-camel-toolbox-app/target/fcrepo-camel-toolbox-app-<verion>-driver.jar -c /path/to/configuration.properties
``` 

where your `configuration.properties `file is a standard java properties file containing key value pairs. To run with the above solr and fuseki docker containers  set the following properties

```
triplestore.indexer.enabled=true
solr.indexer.enabled=true
triplestore.baseUrl=http://localhost:3030/test
solr.baseUrl=http://localhost:8983/solr/
```

## Properties
| Name      | Description| Required | Default Value | Values |
| :---      | :---| :---:  |    :----   | --- |
| Fedora Service|
| fcrepo.baseUrl | The base url endpoint for your Fedora installation.  | no       | http://localhost:8080/fcrepo/rest | Any valid fcrepo url
| fcrepo.authUsername | A valid username      | no       | fcrepoAdmin | | 
| fcrepo.authPassword | A valid password      | no       | fcrepoAdmin | | 
| fcrepo.authHost | The hostname of the Fedora installation which the authUsername and authPassword should be applied to | no       | localhost | | 
| fcrepo.authPort |       | no       | 8080 | | 
| Triplestore Service|
| triplestore.indexer.enabled | Enables the triplestore indexing service. Disabled by default | no | false | 
| triplestore.baseUrl | Base URL for the triplestore | no | http://localhost:8080/fuseki/test/update | 
| triplestore.input.stream |   The JMS topic or queue serving as the message source    | no       | broker:topic:fedora | | 
| triplestore.reindex.stream |   The JMS topic or queue serving as the reindex message source    | no       | broker:queue:solr.reindex | | 
| triplestore.indexing.predicate |   ?    | no       | false | | 
| triplestore.filter.containers |   A comma-separate list of containers that should be ignored by the indexer  | no       | http://localhost:8080/fcrepo/rest/audit | | 
| triplestore.namedGraph |  ?  | no       | null | | 
| triplestore.prefer.include |  ?  | no       | null | | 
| triplestore.prefer.omit |  ?  | no       | http://www.w3.org/ns/ldp#PreferContainment | | 
| SOLR Service|
| solr.indexer.enabled | Enables/disables the SOLR indexing service. Disabled by default | no | false | 
| error.maxRedeliveries |       | no       | 10 | | 
| fcrepo.checkHasIndexingTransformation |       | no       | true | | 
| fcrepo.defaultTransform |   ?    | no       | null | | 
| input.stream |   The JMS topic or queue serving as the message source    | no       | broker:topic:fedora | | 
| solr.reindex.stream |   The JMS topic or queue serving as the reindex message source    | no       | broker:queue:solr.reindex | | 
| solr.commitWithin |   Milliseconds within which commits should occur    | no       | 10000 | | 
| indexing.predicate |   ?    | no       | false | | 
| ldpath.service.baseUrl |   The LDPath service base url    | no       | http://localhost:9085/ldpath | | 
| filter.containers |   A comma-separate list of containers that should be ignored by the indexer  | no       | http://localhost:8080/fcrepo/rest/audit | | 
| LDPath Service | 
| fcrepo.cache.timeout | The timeout in seconds for the ldpath cache | no       | 0 | | 
| rest.prefix | The LDPath rest endpoint prefix |  no | /ldpath| |
| rest.port| The LDPath rest endpoint port |  no | 9085 |
| rest.host| The LDPath rest endpoint host |  no | localhost |
| cache.timeout | LDCache ?  timeout in seconds  |  no | 86400  | |
| ldcache.directory | LDCache directory  |  no | ldcache/  | |
| ldpath.transform.path | The LDPath transform file path | no | classpath:org/fcrepo/camel/ldpath/default.ldpath | For local file paths use `file:` prefix. e.g `file:/path/to/your/transform.txt` |
| Reindexing Service |
| reindexing.enabled | Enables/disables the reindexing component. Enabled by default | no | true | 
| reindexing.error.maxRedeliveries | Maximum redelivery attempts | no | 10 | 
| reindexing.stream | Reindexing jms message stream | no | broker:queue:reindexing | 
| reindexing.host | Reindexing service host | no | localhost | 
| reindexing.port | Reindexing service port | no | 9080 |
| reindexing.rest | Reindexing rest URI prefix | no | /reindexing | 
| ActiveMQ Service |
| jms.brokerUrl | JMS Broker endpoint | no | tcp://localhost:61616 |
| jms.username | JMS username | no | null |
| jms.password | JMS password | no | null |
| jms.connections | The JMS connection count | no | 10 |
| jms.consumers | The JMS consumer count | no | 1 |

TODO:  clean up and regularize property names. 

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

### Repository Indexer (Triplestore) (not currently available)

This application listens to Fedora's event stream and
indexes objects into an external triplestore.

### Fixity Checking Service (not currently available

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

## Maintainers

Current maintainers:

* [Danny Bernstein](https://github.com/dbernstein)
