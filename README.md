Fcrepo Component
================

The **fcrepo:** component provides access to external [Fedora4](http://fcrepo.org) resources.

URI format
----------

    fcrepo:hostname[:port][/resourceUrl][?options]

By default this endpoint connects to fedora repositories on port 80.

FcrepoEndpoint Options
-----------------------

Endpoint options.

| Name         |  Default Value | Description |
| ------------ | -------------- | ----------- |
| `type`         | `null`           | The requested contentType for content negotiation |
| `metadata`     | `true`           | Whether GET requests should only retrieve object metadata |
| `throwExceptionOnFailure` | `true` | Option to disable throwing the HttpOperationFailedException in case of failed responses from the remote server. This allows you to get all responses regardless of the HTTP status code. |

Setting Basic Authentication
----------------------------

| Name         | Default Value | Description |
| ------------ | ------------- | ----------- |
| `authUsername` | `null`          | Username for authentication |
| `authPassword` | `null`          | Password for authentication |
| `authHost`     | `null`          | The host name for authentication |

Message Headers
---------------

| Name     | Type   | Description |
| -------- | ------ | ----------- |
| `Exchange.HTTP_METHOD` | `String` | The HTTP method to use |
| `Exchange.CONTENT_TYPE` | `String` | The ContentType of the resource. For GET requests, this sets the Accept: header; for POST/PUT requests, it sets the Content-Type: header. This value can be overridden directly on the endpoint. |
| `FCREPO_IDENTIFIER`    | `String` | The resource path |

The `fcrepo` component will also accept message headers produced directly by fedora, particularly the `org.fcrepo.jms.identifier` header. It will use that header only when `FEDORA_IDENTIFIER` is not defined.

Message Body
------------

Camel will store the HTTP response from the Fedora4 server on the OUT body. All headers from the
IN message will be copied to the OUT message, so headers are preserved during routing. Additionally,
Camel will add the HTTP response headers to the OUT message headers.

Response code
-------------

Camel will handle the HTTP response code in the following ways:

* Response code in the range 100..299 is a success.
* Response code in the range 300..399 is a redirection and will throw a `HttpOperationFailedException` with the relevant information.
* Response code is 400+ is regarded as an external server error and will throw an `HttpOperationFailedException` with the relevant information.

HttpOperationFailedException
----------------------------

This exception contains the following information:

* The HTTP status code
* The HTTP status line (text of the status code)
* Redirect location, if the server returned a redirect
* Response body as a `java.lang.String`, if server provided a body as response

How to set the HTTP Method
--------------------------

The endpoint will always use the `GET` method unless explicitly set in the `Exchange.HTTP_METHOD` header.
Other methods, such as `PUT`, `PATCH`, `POST`, and `DELETE` are available and will be passed through 
to the Fedora server. Here is an example:

    from("direct:start")
        .setHeader(Exchange.HTTP_HEADER, constant("POST"))
        .to("fcrepo://localhost:8080/fcrepo4/rest")
        .to("mock:results");

And the equivalent Spring sample:

    <camelContext xmlns="http://activemq.apache.org/camel/schema/spring">
      <route>
        <from uri="direct:start"/>
        <setHeader headerName="Exchange.HTTP_HEADER">
            <constant>POST</constant>
        </setHeader>
        <to uri="fcrepo://localhost:8080/fcrepo4/rest"/>
        <to uri="mock:results"/>
      </route>
    </camelContext>

Getting the response code
-------------------------

You can get the HTTP response code from the `fcrepo` component by getting the value from the Out message header with `Exchange.HTTP_RESPONSE_CODE`.

Questions
---------

Feel free to send me an email (acoburn@apache.org) with any questions.

