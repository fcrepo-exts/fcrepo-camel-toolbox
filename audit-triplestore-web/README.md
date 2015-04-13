#Web-Deployable Fedora Audit Service (Triplestore)

This application implements a bridge to an external, triplestore-based
[Audit Service](https://wiki.duraspace.org/display/FF/Design+-+Audit+Service)
for [Fedora4](http://fcrepo.org).

**This application is currently in development**

##Building

To build this project use

    mvn install

from the base directory 

##Running from the command line

To run the project you can execute the following Maven goal

    mvn jetty:run -Djetty.port=9999

##Deploying in Tomcat/Jetty

This project can be deployed in a JVM web container such as Tomcat or Jetty
by copying the `war` file from the `./target/` directory into the container's
application directory.

##Configuration

All configurable properties are stored in the `WEB-INF/classes/application.properties` file.

In addition, many of these can be set from the external environment.

In the event of failure, the maximum number of times a redelivery will be attempted.

    fcrepo.audit.redeliveries (default=10)

The baseUri to use for event URIs in the triplestore. A `UUID` will be appended
to this value, forming, for instance: `http://example.com/event/{UUID}`

    fcrepo.audit.baseUri (default=http://example.com/event)

The hostname for the JMS broker

    fcrepo.jms.host (default=localhost)

The port for the JMS broker

    fcrepo.dynamic.jms.port (default=61616)

The name of the JMS topic (or queue) from which the event stream is to be read.

    fcrepo.jms.endpoint (default=topic:fedora)

The base URL of the triplestore being used.

    fcrepo.audit.triplestore.baseUrl (default=localhost:3030/test/update)

