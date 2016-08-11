#Fedora Serialization Service

This application implements serialization feature for fedora objects that
can be used to serialize objects in a plain text format to disk.


##Building

To build this project use

    MAVEN_OPTS="-Xmx1024m" mvn install

##Deploying in OSGi

This project can be deployed in an OSGi container. For example using
[Apache ServiceMix](http://servicemix.apache.org/) or
[Apache Karaf](http://karaf.apache.org). You can run the following
command from its shell:

    feature:repo-add mvn:org.fcrepo.camel/toolbox-features/LATEST/xml/features
    feature:install fcrepo-serialization
    feature:install fcrepo-service-activemq

##Configuration

This application can be configured by creating a file in
`$KARAF_HOME/etc/org.fcrepo.camel.serialization.cfg`. The following
values are available for configuration:

In the event of failure, the maximum number of times a re-delivery will be attempted.

    error.maxRedeliveries=10

The camel URI for the incoming message stream.

    input.stream=broker:topic:fedora

The camel URI for handling re-serialization events.

    serialization.stream=broker:queue:serialization

The directory to store the metadata files in.

    serialization.descriptions=file:///tmp/descriptions

The directory to store the binary files in, if writing them to disk.

    serialization.binaries=file:///tmp/binaries

The flag for whether or not to write binaries to disk. If you would
like to include binaries in serialization, set this property to `true`.

    serialization.includeBinaries=false

The format the metadata files will be written in.

    serialization.mimeType=text/turtle

The file extension that will be used for the metadata files.

    serialization.extension=ttl

The location of the internal Audit trail if using the `fcrepo-audit` extension module.
Nodes at this location will not be serialized.

    audit.container=/audit

By editing this file, any currently running routes will be immediately redeployed
with the new values.

For more help see the Apache Camel documentation

    http://camel.apache.org/

