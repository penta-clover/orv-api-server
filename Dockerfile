FROM openjdk:21-slim
EXPOSE 8080
ADD build/libs/api-0.0.1-SNAPSHOT.jar spring-app.jar
ENTRYPOINT ["java", "-jar", "spring-app.jar"]
