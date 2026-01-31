FROM eclipse-temurin:21-jre
EXPOSE 8080

# Install FFmpeg
RUN apt-get update && \
    apt-get install -y ffmpeg && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

ADD orv-app/build/libs/orv-app-0.0.1-SNAPSHOT.jar spring-app.jar
ENTRYPOINT ["java", \
    "-XX:+HeapDumpOnOutOfMemoryError", \
    "-XX:HeapDumpPath=/var/log/app/heapdump.hprof", \
    "-jar", "spring-app.jar"]
