# Fedora Audit Service (Triplestore)

This application implements a bridge to an external, triplestore-based
[Audit Service](https://wiki.duraspace.org/display/FF/Design+-+Audit+Service)
for [Fedora4](http://fcrepo.org).

## Configuration

A comma-delimited list of URIs to filter. That is, any Fedora resource that either
matches or is contained in one of the URIs listed will not be processed by the
audit-triplestore application.

    audit.filter.containers=http://localhost:8080/fcrepo/rest/audit

In the event of failure, the maximum number of times a redelivery will be attempted.

    error.maxRedeliveries=10

The baseUri to use for event URIs in the triplestore. A `UUID` will be appended
to this value, forming, for instance: `http://example.com/event/{UUID}`

    audit.event.baseUri=http://example.com/event

The camel URI for the incoming message stream (e.g. with the ActiveMQ service).

    audit.input.stream=broker:topic:fedora

The base URL of the triplestore being used.

    audit.triplestore.baseUrl=http://localhost:8080/fuseki/test/update


By editing this file, any currently running routes will be immediately redeployed
with the new values.

For more help see the Apache Camel documentation

    http://camel.apache.org/

