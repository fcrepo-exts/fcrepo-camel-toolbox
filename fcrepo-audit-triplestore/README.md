#Fedora Audit Service (Triplestore)

This application implements a bridge to an external, triplestore-based
[Audit Service](https://wiki.duraspace.org/display/FF/Design+-+Audit+Service)
for [Fedora4](http://fcrepo.org).

**This application is currently in development**

##Building

To build this project use

    mvn install

##Running from the command line

To run the project you can execute the following Maven goal

    mvn camel:run

##Deploying in OSGi

This project can be deployed in an OSGi container. For example using
[Apache ServiceMix](http://servicemix.apache.org/) or 
[Apache Karaf](http://karaf.apache.org), you can run the following
command from its shell:

    osgi:install -s mvn:org.fcrepo.camel/audit-triplestore/{VERSION}

Or by copying the compiled bundle into `$KARAF_HOME/deploy`.

##Configuration

The application can be configured by creating the following configuration
file `$KARAF_HOME/etc/org.fcrepo.camel.audit.cfg`. The following values
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


By editing this file, any currently running routes will be immediately redeployed
with the new values.

For more help see the Apache Camel documentation

    http://camel.apache.org/

