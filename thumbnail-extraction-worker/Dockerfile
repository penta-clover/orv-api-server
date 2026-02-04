FROM eclipse-temurin:21-jre

# Install FFmpeg
RUN apt-get update && \
    apt-get install -y ffmpeg && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

ADD build/libs/thumbnail-extraction-worker-0.0.1-SNAPSHOT.jar worker-app.jar
ENTRYPOINT ["java", \
    "-XX:+HeapDumpOnOutOfMemoryError", \
    "-XX:HeapDumpPath=/var/log/app/heapdump.hprof", \
    "-Djava.net.preferIPv6Addresses=true", \
    "-jar", "worker-app.jar"]
