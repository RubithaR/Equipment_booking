FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY smartlab-security-commons/ smartlab-security-commons/
COPY smartlab-notification-client/ smartlab-notification-client/
RUN cd smartlab-security-commons && mvn install -DskipTests -B \
 && cd ../smartlab-notification-client && mvn install -DskipTests -B
COPY user-service/pom.xml user-service/pom.xml
COPY user-service/src user-service/src
RUN cd user-service && mvn package -DskipTests -B

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/user-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","/app/app.jar"]
