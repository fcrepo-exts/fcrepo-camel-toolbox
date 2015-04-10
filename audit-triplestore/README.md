#Fedora Audit Service (Triplestore)

This application implements a bridge to an external, triplestore-based
[Audit Service](https://wiki.duraspace.org/display/FF/Design+-+Audit+Service)
for [Fedora4](http://fcrepo.org).

**This application is currently in development**

##Building

To build this project use

    mvn install

##Running from the command line

To run the project you can execute the following Maven goal

    mvn camel:run

##Deploying in OSGi

This project can be deployed in an OSGi container. For example using
[Apache ServiceMix](http://servicemix.apache.org/) or 
[Apache Karaf](http://karaf.apache.org), you can run the following
command from its shell:

    osgi:install -s mvn:org.fcrepo.camel/audit-triplestore/{VERSION}

For more help see the Apache Camel documentation

    http://camel.apache.org/

