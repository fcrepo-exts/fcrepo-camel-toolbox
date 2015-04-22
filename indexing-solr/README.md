#Fedora Indexing Service (Solr)

This application implements a bridge to an external, solr index
for [Fedora4](http://fcrepo.org).

##Building

To build this project use

    MAVEN_OPTS="-Xmx1024m -XX:MaxPermSize=512m" mvn install

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

