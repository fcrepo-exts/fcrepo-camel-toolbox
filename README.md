FCREPO Component
================

The *fcrepo:* component provides access to external Fedora4 resources.

URI format
----------

    fcrepo:hostname[:port][/resourceUrl][?options]

By default this uses port 80.

FcrepoEndpoint Options
-----------------------

Endpoint options.

| Name         |  Default Value | Description |
| ------------ | -------------- | ----------- |
| authUsername | null           | Username for authentication |
| authPassword | null           | Password for authentication |
| authHost     | null           | The host name for authentication |
| type         | null           | The requested contentType for content negotiation |
| metadata     | true           | Whether GET requests should only retrieve object metadata |



