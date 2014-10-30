Fcrepo Component
================

The **fcrepo:** component provides access to an external [Fedora4](http://fcrepo.org) Object [API](https://wiki.duraspace.org/display/FF/RESTful+HTTP+API+-+Objects) for use with [Apache Camel](https://camel.apache.org).


URI format
----------

    fcrepo:hostname[:port][/resourceUrl][?options]

By default this endpoint connects to fedora repositories on port 80.


FcrepoEndpoint Options
-----------------------

Endpoint options.

| Name         |  Default Value | Description |
| ------------ | -------------- | ----------- |
| `contentType`       | `null`         | The requested contentType for content negotiation |
| `useRdfDescription` | `true`  | Whether GET requests should retrieve RDF descriptions of non-RDF content  |
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


HttpOperationFailedException
----------------------------

This exception contains the following information:

* The HTTP status code
* The HTTP status line (text of the status code)
* Redirect location, if the server returned a redirect
* Response body as a `java.lang.String`, if server provided a body as response


How to set the HTTP Method
--------------------------

The endpoint will always use the `GET` method unless explicitly set
in the `Exchange.HTTP_METHOD` header. Other methods, such as `PUT`,
`PATCH`, `POST`, and `DELETE` are available and will be passed through 
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

You can get the HTTP response code from the `fcrepo` component by getting
the value from the Out message header with `Exchange.HTTP_RESPONSE_CODE`.


Building the component
----------------------

The `fcrepo-camel` compnent can be built with Maven:

    mvn clean install

The tests assume that fedora4 is already running at `http://localhost:8080/fcrepo4/rest`.
This value can be overridden like so:

    mvn clean install -Dfcrepo.url=http://localhost:8983/rest 

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

###Distributed Messaging Deployments

The default configuration is fine for locally-deployed listeners, but it can
be problematic in a distributed context. For instance, if the listener is 
restarted while a message is sent to the topic, that message will be missed. 
Furthermore, if there is a networking hiccup between fedora's local broker 
and the remote listener, that too can result in lost messages. Instead, in 
this case, a queue may be better suited.

####Supporting Queues

ActiveMQ supports “[virtual destinations](http://activemq.apache.org/virtual-destinations.html)”,
allowing your broker to automatically forward messages from one
location to another. If fedora4 is deployed in Tomcat, the ActiveMQ
configuration will be located in `WEB-INF/classes/config/activemq.xml`.
That file can be edited to include the following block:

    <destinationInterceptors>
      <virtualDestinationInterceptor>
        <virtualDestinations>
          <compositeTopic name="fedora">
            <forwardTo>
              <queue physicalName="fedora"/>
            </forwardTo>
          </compositeTopic>
        </virtualDestinations>
      </virtualDestinationInterceptor>
    </destinationInterceptors>

Now a consumer can pull messages from a queue without risk of losing messages.

This configuration, however, will not allow any other applications to read from
the original topic. If it is necessary to have `/topic/fedora` available to
consumers, this configuration will be useful:

    <destinationInterceptors>
      <virtualDestinationInterceptor>
        <virtualDestinations>
          <compositeTopic name="fedora" forwardOnly="false">
            <forwardTo>
              <queue physicalName="fedora"/>
            </forwardTo>
          </compositeTopic>
        </virtualDestinations>
      </virtualDestinationInterceptor>
    </destinationInterceptors>

Now, both `/topic/fedora` and `/queue/fedora` will be available to consumers.

####Distributed Brokers

The above example will allow you to distribute the message consumers across
multiple machines without missing messages, but it can also be useful to
distribute the message broker across multiple machines. This can be especially
useful if you want to further decouple the message producers and consumers.
It can also be useful for high-availability and failover support.

ActiveMQ supports a variety of distributed broker
[topologies](http://activemq.apache.org/topologies.html). To push messages
from both the message queue and topic to a remote broker, this configuration
can be used:

    <networkConnectors>
      <networkConnector name="fedora_bridge" dynamicOnly="true" uri="static:(tcp://remote-host:61616)">
        <dynamicallyIncludedDestinations>
          <topic physicalName="fedora"/>
          <queue physicalName="fedora"/>
        </dynamicallyIncludedDestinations>
      </networkConnector>
    </networkConnectors>

###Protocol Support

ActiveMQ brokers support a wide variety of protocols. The default Fedora4
configuration includes [OpenWire](http://activemq.apache.org/openwire.html)
and [Stomp](http://stomp.github.io/). If Fedora's internal broker is
bridged to an external broker, please remember to enable the proper
protocols on the remote broker. This can be done like so:

    <transportConnectors>
      <transportConnector name="openwire" uri="tcp://0.0.0.0:61616"/>
      <transportConnector name="stomp" uri="stomp://0.0.0.0:61613"/>
    </transportConnectors>

Each transportConnector supports additional [options](http://activemq.apache.org/configuring-transports.html).

Questions
---------

Feel free to send me an email (acoburn@apache.org) with any questions.

