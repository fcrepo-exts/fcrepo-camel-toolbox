# Build
FROM maven:3-openjdk-11 as build

COPY . /home/fcrepo-camel-toolbox/
RUN mvn -f /home/fcrepo-camel-toolbox/pom.xml clean package

# Run
FROM openjdk:11-jre-slim
RUN mkdir /config
COPY --from=build /home/fcrepo-camel-toolbox/fcrepo-camel-toolbox-app/target/fcrepo-camel-toolbox-app-6.0.0-SNAPSHOT-driver.jar /usr/local/fcrepo-camel-toolbox/driver.jar

ENTRYPOINT ["java", "-jar", "/usr/local/fcrepo-camel-toolbox/driver.jar", "-c", "/config/configuration.properties"]
