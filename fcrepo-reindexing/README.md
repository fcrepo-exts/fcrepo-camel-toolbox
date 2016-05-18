#Fedora Reindexing Service

This application implements a reindexing service so that
any node hierarchy in fedora (e.g. the entire repository
or some subset thereof) can be reindexed by a set of external
services.

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
    feature:install fcrepo-reindexing
    feature:install fcrepo-service-activemq

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

The camel URI for the internal reindexing queue.

    reindexing.stream=broker:queue:reindexing

The host to which to bind the HTTP endpoint

    rest.host=localhost

The port at which reindexing requests can be sent.

    rest.port=9080

The URL path prefix for the reindexing service.

    rest.prefix=/reindexing

By editing this file, any currently running routes will be immediately redeployed
with the new values.

For more help see the Apache Camel documentation

    http://camel.apache.org/

