# syntax=docker/dockerfile:1.7
# ===== Stage 1: build the fat jar =====
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2,id=smartlab-m2 \
    mvn dependency:go-offline -B
COPY src src
RUN --mount=type=cache,target=/root/.m2,id=smartlab-m2 \
    mvn package -DskipTests -B

# ===== Stage 2: slim runtime =====
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java","-jar","/app/app.jar"]
