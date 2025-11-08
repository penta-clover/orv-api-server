FROM eclipse-temurin:21-jre-alpine
EXPOSE 8080
ADD build/libs/api-0.0.1-SNAPSHOT.jar spring-app.jar
ENTRYPOINT ["java", "-jar", "spring-app.jar"]
