# syntax=docker/dockerfile:1.7
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY smartlab-security-commons/ smartlab-security-commons/
RUN --mount=type=cache,target=/root/.m2,id=smartlab-m2 \
    cd smartlab-security-commons && mvn install -DskipTests -B

COPY equipment-service/pom.xml equipment-service/pom.xml
RUN --mount=type=cache,target=/root/.m2,id=smartlab-m2 \
    cd equipment-service && mvn dependency:go-offline -B
COPY equipment-service/src equipment-service/src
RUN --mount=type=cache,target=/root/.m2,id=smartlab-m2 \
    cd equipment-service && mvn package -DskipTests -B

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/equipment-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java","-jar","/app/app.jar"]
