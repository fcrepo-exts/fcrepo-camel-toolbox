#!/bin/sh

exec java ${JAVA_OPTIONS} -jar /usr/local/fcrepo-camel-toolbox/driver.jar "$@"
