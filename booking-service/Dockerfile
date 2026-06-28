# syntax=docker/dockerfile:1.7
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY smartlab-security-commons/ smartlab-security-commons/
COPY smartlab-notification-client/ smartlab-notification-client/
RUN --mount=type=cache,target=/root/.m2,id=smartlab-m2 \
    cd smartlab-security-commons && mvn install -DskipTests -B \
 && cd ../smartlab-notification-client && mvn install -DskipTests -B

COPY booking-service/pom.xml booking-service/pom.xml
RUN --mount=type=cache,target=/root/.m2,id=smartlab-m2 \
    cd booking-service && mvn dependency:go-offline -B
COPY booking-service/src booking-service/src
RUN --mount=type=cache,target=/root/.m2,id=smartlab-m2 \
    cd booking-service && mvn package -DskipTests -B

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/booking-service/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java","-jar","/app/app.jar"]
