FROM maven:3-eclipse-temurin-11 AS build

WORKDIR /build

COPY . ./

# Build with maven, if fcrepo-camel-toolbox-app-*-driver.jar does not exist:
RUN [ -e "fcrepo-camel-toolbox-app/target/"fcrepo-camel-toolbox-app-*-driver.jar ] || mvn clean package

FROM eclipse-temurin:11-jre AS app

WORKDIR /usr/local/fcrepo-camel-toolbox

COPY --from=build "/build/fcrepo-camel-toolbox-app/target/fcrepo-camel-toolbox-app-*-driver.jar" ./driver.jar

COPY docker/entrypoint.sh ./
RUN chmod a+x ./entrypoint.sh

ENTRYPOINT ["./entrypoint.sh"]
