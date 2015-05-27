#Fedora Reindexing Service

This application implements a reindexing service so that
any node hierarchy in fedora (e.g. the entire repository
or some subset thereof) can be reindexed by a set of external
services.

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
    feature:install fcrepo-reindexing

##Configuration

The application can be configured by creating a file in
`$KARAF_HOME/etc/org.fcrepo.camel.reindexing.cfg`. The following
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

The JMS connection URI, used for connecting to a local or remote ActiveMQ broker.

    jms.brokerUrl=tcp://localhost:61616

The camel URI for the internal reindexing queue.

    reindexing.stream=activemq:queue:reindexing

The port at which reindexing requests can be sent.

    rest.port=9080

The URL path prefix for the reindexing service.

    rest.prefix=/reindexing

By editing this file, any currently running routes will be immediately redeployed
with the new values.

For more help see the Apache Camel documentation

    http://camel.apache.org/

