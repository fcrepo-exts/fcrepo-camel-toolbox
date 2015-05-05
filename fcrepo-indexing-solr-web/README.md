#Web-Deployable Solr Indexer

This application implements a bridge to an external, solr-based index
for [Fedora4](http://fcrepo.org).

##Building

To build this project use

    mvn install

from the base directory 

##Running from the command line

To run the project you can execute the following Maven goal

    mvn jetty:run -Djetty.port=9999 -Dfcrepo.baseUrl=localhost:8080/fcrepo/rest -Dsolr.baseUrl=localhost:8983/solr/core1

##Deploying in Tomcat/Jetty

This project can be deployed in a JVM web container such as Tomcat or Jetty
by copying the `war` file from the `./target/` directory into the container's
application directory.

##Configuration

All configurable properties are stored in the `WEB-INF/classes/application.properties` file.

In addition, many of these can be set from the external environment.

In the event of failure, the maximum number of times a redelivery will be attempted.

    fcrepo.indexing.redeliveries (default=10)

The hostname for the JMS broker

    fcrepo.jms.host (default=localhost)

The port for the JMS broker

    fcrepo.dynamic.jms.port (default=61616)

The name of the JMS topic (or queue) from which the event stream is to be read.

    fcrepo.jms.endpoint (default=topic:fedora)

The base URL of the Fedora server.

    fcrepo.baseUrl (default=localhost:8080/rest)

The username for Fedora authentication

    fcrepo.authUsername

The password for Fedora authentication

    fcrepo.authPassword

The host realm for Fedora authentication

    fcrepo.authHost

The default LDPath transformation (`fcr:transform`) for Fedora objects

    fedora.defaultTransform (default=default)

Whether to index only objects with an `indexing:Indexable` type

    fcrepo.onlyIndexableObjects (default=false)

The base URL of the Solr server.

    solr.baseUrl (default=localhost:8983/solr)

The frequency with which autocommits are executed by Solr
    
    solr.commitWithin (default=10000)



