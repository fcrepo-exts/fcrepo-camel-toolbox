# Fedora Audit Service (Triplestore)

This service listens for repository messages (create, update, delete) from Fedora, translates them into RDF and logs them to  an external triple store thereby creating an audit log of all changes to the Fedora repository.

## Configuration

   audit.enabled=false

Set this property to true to enable the audit service. 

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

    audit.triplestore.baseUrl=http://localhost:3030/fuseki/test/update

For more help see the Apache Camel documentation

    http://camel.apache.org/

