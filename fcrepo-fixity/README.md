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

For more help see the Apache Camel documentation

    http://camel.apache.org/

