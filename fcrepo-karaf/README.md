#Karaf Provisioning for fcrepo-camel-toolbox

The `fcrepo-camel-toolbox` recipes are built and distributed as OSGi modules.
This module makes it convenient to deploy these artifacts in an OSGi runtime
such as Apache Karaf.

In order to deploy the projects from `fcrepo-camel-toolbox` in Karaf, you can
run this command in the Karaf console:

    feature:repo-add mvn:org.fcrepo.camel/fcrepo-karaf/LATEST/xml/features

To list the various `fcrepo-*` features available, use this command:

    feature:list | grep fcrepo

And installing a feature uses a command such as the following:

    feature:install fcrepo-fixity
