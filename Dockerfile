FROM openjdk:11-jre-slim

COPY docker/entrypoint.sh /usr/local/bin/
RUN chmod a+x /usr/local/bin/entrypoint.sh

WORKDIR /usr/local/fcrepo-camel-toolbox

ARG FCREPO_CAMEL_TOOLBOX_VERSION=6.0.0-SNAPSHOT
COPY fcrepo-camel-toolbox-app/target/fcrepo-camel-toolbox-app-${FCREPO_CAMEL_TOOLBOX_VERSION}-driver.jar driver.jar

ENV FCREPO_CAMEL_TOOLBOX_HOME=/usr/local/fcrepo-camel-toolbox

ENTRYPOINT ["entrypoint.sh"]
