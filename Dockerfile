FROM eclipse-temurin:21-jre
EXPOSE 8080

# Install FFmpeg
RUN apt-get update && \
    apt-get install -y ffmpeg libjemalloc2 && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* && \
    ln -s /usr/lib/$(dpkg --print-architecture)-linux-gnu/libjemalloc.so.2 /usr/local/lib/libjemalloc.so.2

ENV LD_PRELOAD=/usr/local/lib/libjemalloc.so.2

ADD orv-app/build/libs/orv-app-0.0.1-SNAPSHOT.jar spring-app.jar
ENTRYPOINT ["java", \
    "-XX:+HeapDumpOnOutOfMemoryError", \
    "-XX:HeapDumpPath=/var/log/app/heapdump.hprof", \
    "-jar", "spring-app.jar"]
