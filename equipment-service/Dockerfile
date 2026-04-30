FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY smartlab-security-commons/ smartlab-security-commons/
RUN cd smartlab-security-commons && mvn install -DskipTests -B
COPY equipment-service/pom.xml equipment-service/pom.xml
COPY equipment-service/src equipment-service/src
RUN cd equipment-service && mvn package -DskipTests -B

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/equipment-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java","-jar","/app/app.jar"]
