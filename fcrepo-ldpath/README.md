#Fedora LDPath Service

This application implements an LDPath service on repository
resources. This allows users to dereference and follow URI
links to arbitrary lengths. Retrieved triples are cached locally
for a specified period of time.

More information about LDPath can be found at the [Marmotta website](http://marmotta.apache.org/ldpath/language.html).

Additional background information on this service is available on the Fedora Wiki on the
[Integration Services page](https://wiki.duraspace.org/display/FEDORA4x/Integration+Services).

Note: The LDPath service requires an LDCache backend, such as `fcrepo-service-ldcache-file`.

## Usage

The LDPath service responds to `GET` and `POST` requests using any accessible resources as a context.

For example, a request to
`http://localhost:9086/ldpath/?context=http://localhost/rest/path/to/fedora/object`
will apply the appropriate ldpath program to the specified resource. Note: it is possible to
identify non-Fedora resources in the context parameter.

A `GET` request can include a `ldpath` parameter, pointing to the URL location of an LDPath program:

    `curl http://localhost:9086/ldpath/?context=http://localhost/rest/path/to/fedora/object&ldpath=http://example.org/ldpath`

Otherwise, it will use a simple default ldpath program.

A `POST` request can also be accepted by this endpoint. The body of a `POST` request should contain
the entire `LDPath` program. The `Content-Type` of the request should be either `text/plain` or
`application/ldpath`.

    `curl -XPOST -H"Content-Type: application/ldpath" -d @program.txt http://localhost:9086/ldpath/?context=http://localhost/rest/path/to/fedora/object


## Building

To build this project use

    MAVEN_OPTS="-Xmx1024m" mvn install

## Deploying in OSGi

This project can be deployed in an OSGi container. For example using
[Apache ServiceMix](http://servicemix.apache.org/) or
[Apache Karaf](http://karaf.apache.org), you can run the following
command from its shell:

    feature:repo-add mvn:org.fcrepo.camel/toolbox-features/LATEST/xml/features
    feature:install fcrepo-service-ldcache-file
    feature:install fcrepo-ldpath

## Configuration

The application can be configured by creating a file in
`$KARAF_HOME/etc/org.fcrepo.camel.ldpath.cfg`. The following
values are available for configuration:

If the fedora repository requires authentication, the following values
can be set:

    fcrepo.authUsername=<username>
    fcrepo.authPassword=<password>
    fcrepo.authHostname=localhost
    fcrepo.authPort=8080

The baseUrl for the fedora repository.

    fcrepo.baseUrl=http://localhost:8080/fcrepo/rest

The time Fedora triples are cached (in seconds)

    fcrepo.cache.timeout=0

The global timeout for cache entries (in seconds)

    cache.timeout=86400

The host to which to bind the HTTP endpoint

    rest.host=localhost

The port at which ldpath requests can be sent.

    rest.port=9086

The URL path prefix for the ldpath service.

    rest.prefix=/ldpath

By editing this file, any currently running routes will be immediately redeployed
with the new values.

For more help see the Apache Camel documentation

    http://camel.apache.org/

