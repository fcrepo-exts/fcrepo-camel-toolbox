#Fedora Serialization Service

This application implements serialization feature for fedora objects that 
can be used to serialize objects in a plain text format to disk. 


##Building 

To build this project use

    MAVEN_OPTS="-Xmx1024m" mvn install

##Running from the command line

To run the project you can execute the following Maven goal

    mvn camel:run

##Deploying in OSGi

This project can be deployed in an OSGi container. For example using 
[Apache ServiceMix](http://servicemix.apache.org/) or
[Apache Karaf](http://karaf.apache.org). You can run the following 
command from its shell:

    feature:repo-add mvn:org.fcrep.camel/fcrepo-camel-toolbox/LATEST/xml/features
    feature:install fcrepo-serialization

##Deploying in Tomcat/Jetty

If you intend to deploy this application in a web containeri such as Tomcat or Jetty,
please refer to the documentation in the 
[fcrepo-camel-webapp](https://github.com/fcrepo4-ext/fcrepo-camel-toolbox/tree/master/fcrepo-camel-webapp)
project.

##Configuration

This application can be configured by creating a file in 
`$KARAF_HOME/etc/org.fcrep.camel.serialization.cfg`. The following
values are available for configuration:

In the event of value, the maximum number of times a redelivery will be attempted.
    error.maxRedeliveries=10

If the fedora repositoyr requires authentication, the following values 
can be set: 

    fcrepo.authUsername=<username>
    fcrepo.authPassword=<password>
    fcrepo.authHost=<host realm>

The baseUrl for the fedora reposityr.
    
    fcrepo.baseUrl=localhost:8080/fcrepo/rest

The JMS connection URI, used as a default input stream (queue). 

    jms.brokerUrl=tcp://localhost:61616


For more help see the Apache Camel documentation

    http://camel.apache.org/

