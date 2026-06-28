# syntax=docker/dockerfile:1.7
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY smartlab-security-commons/ smartlab-security-commons/
COPY smartlab-notification-client/ smartlab-notification-client/
RUN --mount=type=cache,target=/root/.m2,id=smartlab-m2 \
    cd smartlab-security-commons && mvn install -DskipTests -B \
 && cd ../smartlab-notification-client && mvn install -DskipTests -B

COPY notification-service/pom.xml notification-service/pom.xml
RUN --mount=type=cache,target=/root/.m2,id=smartlab-m2 \
    cd notification-service && mvn dependency:go-offline -B
COPY notification-service/src notification-service/src
RUN --mount=type=cache,target=/root/.m2,id=smartlab-m2 \
    cd notification-service && mvn package -DskipTests -B

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/notification-service/target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java","-jar","/app/app.jar"]
