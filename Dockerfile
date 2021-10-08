FROM maven:3-openjdk-11-slim AS build

WORKDIR /build

COPY . ./

# Build with maven, if fcrepo-camel-toolbox-app-*-driver.jar does not exist:
RUN [ -e "fcrepo-camel-toolbox-app/target/"fcrepo-camel-toolbox-app-*-driver.jar ] || mvn clean package

FROM openjdk:11-jre-slim AS app

WORKDIR /usr/local/fcrepo-camel-toolbox

COPY --from=build "/build/fcrepo-camel-toolbox-app/target/fcrepo-camel-toolbox-app-*-driver.jar" ./driver.jar

COPY docker/entrypoint.sh ./
RUN chmod a+x ./entrypoint.sh

ENV FCREPO_CAMEL_TOOLBOX_HOME=/usr/local/fcrepo-camel-toolbox

ENTRYPOINT ["./entrypoint.sh"]
