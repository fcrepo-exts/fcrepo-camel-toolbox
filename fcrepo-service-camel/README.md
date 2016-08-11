#Fedora Camel Service

This wraps fcrepo-camel in an OSGi service so that it can be shared among different components,
allowing the configuration to be significantly consolidated.

Additional background information on this service is available on the Fedora Wiki on the
[Integration Services page](https://wiki.duraspace.org/display/FEDORA4x/Integration+Services).

##Building

To build this project use

    MAVEN_OPTS="-Xmx1024m" mvn install

##Deploying in OSGi

This project can be deployed in an OSGi container. For example using
[Apache ServiceMix](http://servicemix.apache.org/) or
[Apache Karaf](http://karaf.apache.org), you can run the following
command from its shell:

    feature:repo-add mvn:org.fcrepo.camel/toolbox-features/LATEST/xml/features
    feature:install fcrepo-service-camel

##Configuration

The application can be configured by creating a file in
`$KARAF_HOME/etc/org.fcrepo.camel.service.cfg`. The following
values are available for configuration:

If the fedora repository requires authentication, the following values
can be set:

    fcrepo.authUsername=<username>
    fcrepo.authPassword=<password>
    fcrepo.authHost=<host realm>

The baseUrl for the fedora repository.

    fcrepo.baseUrl=http://localhost:8080/fcrepo/rest

By editing this file, any currently running routes that rely on the activemq service
will be immediately redeployed using a connector with the new values.

For more help see the Apache Camel documentation

    http://camel.apache.org/

