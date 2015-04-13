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

