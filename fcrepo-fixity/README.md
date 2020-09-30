# Fedora Fixity Service

This application implements a fixity checking service for
[Fedora4](http://fcrepo.org) that can be used to send alerts about
checksum errors.

By default, errors will be saved to a file in `/tmp`, but this can be
modified by specifying an alternate value for `fixity.failure`. It is
also possible to trigger an event on successful fixity checks.

This service is typically used in conjunction with the
[fcrepo-reindexing](https://github.com/fcrepo-exts/fcrepo-camel-toolbox/tree/master/fcrepo-reindexing)
module. For example:

    curl -XPOST localhost:9080/reindexing/fedora/path -H"Content-Type: application/json" \
        -d '["broker:queue:fixity"]'

## Building

To build this project use

    MAVEN_OPTS="-Xmx1024m" mvn install

## Deploying in OSGi

This project can be deployed in an OSGi container. For example using
[Apache ServiceMix](http://servicemix.apache.org/) or
[Apache Karaf](http://karaf.apache.org), you can run the following
command from its shell:

    feature:repo-add mvn:org.fcrepo.camel/toolbox-features/LATEST/xml/features
    feature:install fcrepo-fixity
    feature:install fcrepo-service-activemq

## Configuration

The application can be configured by creating a file in
`$KARAF_HOME/etc/org.fcrepo.camel.fixity.cfg`. The following
values are available for configuration:

In the event of failure, the maximum number of times a redelivery will be attempted.

    error.maxRedeliveries=10

The camel URI for the incoming message stream.

    fixity.stream=broker:queue:fixity

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

By editing this file, any currently running routes will be immediately redeployed
with the new values.

For more help see the Apache Camel documentation

    http://camel.apache.org/

