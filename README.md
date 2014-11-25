Fcrepo Component
================

The **fcrepo:** component provides access to an external
[Fedora4](http://fcrepo.org) Object
[API](https://wiki.duraspace.org/display/FF/RESTful+HTTP+API+-+Objects)
for use with [Apache Camel](https://camel.apache.org).

[![Build Status](https://travis-ci.org/fcrepo4-labs/fcrepo-camel.png?branch=master)](https://travis-ci.org/fcrepo4-labs/fcrepo-camel)

URI format
----------

    fcrepo:hostname[:port][/resourceUrl][?options]

By default this endpoint connects to fedora repositories on port 80.


FcrepoEndpoint options
-----------------------

| Name         |  Default Value | Description |
| ------------ | -------------- | ----------- |
| `contentType`       | `null`         | Set the `Content-Type` header |
| `accept` | `null` | Set the `Accept` header for content negotiation |
| `metadata` | `true`  | Whether GET requests should retrieve RDF descriptions of non-RDF content  |
| `transform` | `null` | If set, this defines the transform used for the given object. This should be used in the context of GET or POST. For GET requests, the value should be the name of the transform (e.g. `default`). For POST requests, the value can simply be `true`. Using this causes the `Accept` header to be set as `application/json`. |
| `throwExceptionOnFailure` | `true` | Option to disable throwing the HttpOperationFailedException in case of failed responses from the remote server. This allows you to get all responses regardless of the HTTP status code. |


Examples
--------

A simple example for sending messages to an external Solr service:

    XPathBuilder xpath = new XPathBuilder("/rdf:RDF/rdf:Description/rdf:type[@rdf:resource='http://fedora.info/definitions/v4/repository#Indexable']");
    xpath.namespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");

    from("activemq:topic:fedora")
      .to("fcrepo:localhost:8080/rest")
      .filter(xpath)
      .to("fcrepo:localhost:8080/rest?transform=mytransform")
      .to("http4:solr-host:8080/solr/core/update")

Or, using the Spring DSL:

    <route id="solr-indexer">
      <from uri="activemq:topic:fedora"/>
      <to uri="fcrepo:localhost:8080/rest"/>
      <filter>
        <xpath>/rdf:RDF/rdf:Description/rdf:type[@rdf:resource='http://fedora.info/definitions/v4/repository#Indexable']</xpath>
        <to uri="fcrepo:localhost:8080/rest?transform=mytransform"/>
        <to uri="http4:solr-host:8080/solr/core/update"/>
      </filter>
    </route>


Setting basic authentication
----------------------------

| Name         | Default Value | Description |
| ------------ | ------------- | ----------- |
| `authUsername` | `null`          | Username for authentication |
| `authPassword` | `null`          | Password for authentication |
| `authHost`     | `null`          | The host name for authentication |


Message headers
---------------

| Name     | Type   | Description |
| -------- | ------ | ----------- |
| `Exchange.HTTP_METHOD` | `String` | The HTTP method to use |
| `Exchange.CONTENT_TYPE` | `String` | The ContentType of the resource. This sets the `Content-Type` header, but this value can be overridden directly on the endpoint. |
| `Exchange.ACCEPT_CONTENT_TYPE` | `String` | This sets the `Accept` header, but this value can be overridden directly on the endpoint. |
| `FCREPO_IDENTIFIER`    | `String` | The resource path, appended to the endpoint uri. |

The `fcrepo` component will also accept message headers produced directly by fedora, particularly the `org.fcrepo.jms.identifier` header. It will use that header only when `FEDORA_IDENTIFIER` is not defined.

Message body
------------

Camel will store the HTTP response from the Fedora4 server on the 
OUT body. All headers from the IN message will be copied to the OUT
message, so headers are preserved during routing. Additionally,
Camel will add the HTTP response headers to the OUT message headers.


Response code
-------------

Camel will handle the HTTP response code in the following ways:

* Response code in the range 100..299 is a success.
* Response code in the range 300..399 is a redirection and will throw a `HttpOperationFailedException` with the relevant information.
* Response code is 400+ is regarded as an external server error and will throw an `HttpOperationFailedException` with the relevant information.

Resource path
-------------

The path for `fcrepo` resources can be set in several different ways. If the
`FCREPO_IDENTIFIER` header is set, that value will be appended to the endpoint
URI. If the `FCREPO_IDENTIFIER` is not set, the path will be populated by the
`org.fcrepo.jms.identifier` header and appended to the endpoint URI. If neither
header is set, only the endpoint URI will be used.

It is generally a good idea to set the endpoint URI to fedora's REST API
endpoint and then use the appropriate header to set the path of the intended
resource.

For example, each of these routes will request the resource at
`http://localhost:8080/rest/a/b/c/abcdef`:

    from("direct:start")
      .setHeader("FCREPO_IDENTIFIER", "/a/b/c/abcdef")
      .to("fcrepo:localhost:8080/rest");

    // org.fcrepo.jms.identifier and FCREPO_IDENTIFIER headers are undefined
    from("direct:start")
      .to("fcrepo:localhost:8080/rest/a/b/c/abcdef");

    // org.fcrepo.jms.identifier is set as '/a/b/c/abcdef'
    // and FCREPO_IDENTIFIER is not defined
    from("direct:start")
      .to("fcrepo:localhost:8080/rest")


HttpOperationFailedException
----------------------------

This exception contains the following information:

* The HTTP status code
* The HTTP status line (text of the status code)
* Redirect location, if the server returned a redirect
* Response body as a `java.lang.String`, if server provided a body as response


How to set the HTTP method
--------------------------

The endpoint will always use the `GET` method unless explicitly set
in the `Exchange.HTTP_METHOD` header. Other methods, such as `PUT`,
`PATCH`, `POST`, and `DELETE` are available and will be passed through 
to the Fedora server. Here is an example:

    from("direct:start")
        .setHeader(Exchange.HTTP_HEADER, constant("POST"))
        .to("fcrepo:localhost:8080/fcrepo4/rest")
        .to("mock:results");

And the equivalent Spring sample:

    <camelContext xmlns="http://activemq.apache.org/camel/schema/spring">
      <route>
        <from uri="direct:start"/>
        <setHeader headerName="Exchange.HTTP_HEADER">
            <constant>POST</constant>
        </setHeader>
        <to uri="fcrepo:localhost:8080/fcrepo4/rest"/>
        <to uri="mock:results"/>
      </route>
    </camelContext>


Getting the response code
-------------------------

You can get the HTTP response code from the `fcrepo` component by getting
the value from the Out message header with `Exchange.HTTP_RESPONSE_CODE`.


Building the component
----------------------

The `fcrepo-camel` compnent can be built with Maven:

    mvn clean install

Fcrepo messaging
----------------

Fedora4 uses an internal [ActiveMQ](https://activemq.apache.org) message
broker to send messages about any updates to the repository content. By
default, all events are published to a `topic` called `fedora` on the
local broker. Each message contains an empty body and five different
header values:

  * `org.fcrepo.jms.identifier`
  * `org.fcrepo.jms.eventType`
  * `org.fcrepo.jms.properties`
  * `org.fcrepo.jms.timestamp`
  * `org.fcrepo.jms.baseURL`

Both `eventType` and `properties` are comma-delimited lists of events or properties.
The `eventType` values follow the JCR 2.0 specification and include:

  * `http://fedora.info/definitions/v4/repository#NODE_ADDED`
  * `http://fedora.info/definitions/v4/repository#NODE_REMOVED`
  * `http://fedora.info/definitions/v4/repository#PROPERTY_ADDED`
  * `http://fedora.info/definitions/v4/repository#PROPERTY_CHANGED`
  * `http://fedora.info/definitions/v4/repository#PROPERTY_REMOVED`

The `properties` field will list the RDF properties that changed with that
event. `NODE_REMOVED` events contain no properties.

Examples and more information
-----------------------------

There are several example projects in the `examples` directory of this distribution.

Furthermore, additional information about designing and deploying **fcrepo**-based message routes along
with configuration options for Fedora's ActiveMQ broker can be found on the
[fedora project wiki](https://wiki.duraspace.org/display/FF/Setup+Camel+Message+Integrations).

