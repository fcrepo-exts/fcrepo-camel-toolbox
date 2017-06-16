# Fedora ActiveMQ Service

This implements a connector to an ActiveMQ broker. It may be used
with other applications in the `fcrepo-camel-toolbox`.

Additional background information on this service is available on the Fedora Wiki on the
[Integration Services page](https://wiki.duraspace.org/display/FEDORA4x/Integration+Services).

## Building

To build this project use

    MAVEN_OPTS="-Xmx1024m" mvn install

## Deploying in OSGi

This project can be deployed in an OSGi container. For example using
[Apache ServiceMix](http://servicemix.apache.org/) or
[Apache Karaf](http://karaf.apache.org), you can run the following
command from its shell:

    feature:repo-add mvn:org.fcrepo.camel/toolbox-features/LATEST/xml/features
    feature:install fcrepo-service-activemq

## Configuration

The application can be configured by creating a file in
`$KARAF_HOME/etc/org.fcrepo.camel.service.activemq.cfg`. The following
values are available for configuration:

The url for connecting to the ActiveMQ broker

    jms.brokerUrl=tcp://localhost:61616

If the ActiveMQ broker requires authentication, these properties will be useful:

    jms.username=<username>
    jms.password=<password>

By editing this file, any currently running routes that rely on the activemq service
will be immediately redeployed using a connector with the new values.

For more help see the Apache Camel documentation

    http://camel.apache.org/

