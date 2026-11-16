FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

COPY . ./

# Build with maven (tests run in CI), if fcrepo-camel-toolbox-app-*-driver.jar does not exist:
RUN [ -e "fcrepo-camel-toolbox-app/target/"fcrepo-camel-toolbox-app-*-driver.jar ] || mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre AS app

WORKDIR /usr/local/fcrepo-camel-toolbox

COPY --from=build "/build/fcrepo-camel-toolbox-app/target/fcrepo-camel-toolbox-app-*-driver.jar" ./driver.jar

COPY docker/entrypoint.sh ./
RUN chmod a+x ./entrypoint.sh

ENTRYPOINT ["./entrypoint.sh"]
