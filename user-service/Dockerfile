# syntax=docker/dockerfile:1.7
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Install shared libs into the Maven cache
COPY smartlab-security-commons/ smartlab-security-commons/
COPY smartlab-notification-client/ smartlab-notification-client/
RUN --mount=type=cache,target=/root/.m2,id=smartlab-m2 \
    cd smartlab-security-commons && mvn install -DskipTests -B \
 && cd ../smartlab-notification-client && mvn install -DskipTests -B

# Cache service deps separately from src/, so editing Java code does not re-download
COPY user-service/pom.xml user-service/pom.xml
RUN --mount=type=cache,target=/root/.m2,id=smartlab-m2 \
    cd user-service && mvn dependency:go-offline -B
COPY user-service/src user-service/src
RUN --mount=type=cache,target=/root/.m2,id=smartlab-m2 \
    cd user-service && mvn package -DskipTests -B

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/user-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","/app/app.jar"]
