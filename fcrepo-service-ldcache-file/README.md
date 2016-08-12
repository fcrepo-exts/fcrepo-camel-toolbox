# File-based LDCache backend

This service provides a file-based ldcache implementation for use with
the `fcrepo-ldpath` service.

## Deploying in OSGi

This project can be deployed in an OSGi container. For example, using
[Apache Karaf](http://karaf.apache.org), you can run the following
command from the shell:

    feature:repo-add mvn:org.fcrepo.camel/toolbox-features/LATEST/xml/features
    feature:install fcrepo-service-ldcache-file
    feature:install fcrepo-ldpath

## Configuration

The application can be configured by creating a file in
`$KARAF_HOME/etc/org.fcrepo.camel.ldcache.file.cfg`. The following
values are available for configuration:

The directory in which the cache is stored. By default the cache will be inside
the karaf data directory.

    ldcache.directory=${karaf.data}/ldcache
