FROM eclipse-temurin:21-jre

# Install FFmpeg
RUN apt-get update && \
    apt-get install -y ffmpeg libjemalloc2 && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* && \
    ln -s /usr/lib/$(dpkg --print-architecture)-linux-gnu/libjemalloc.so.2 /usr/local/lib/libjemalloc.so.2

ENV LD_PRELOAD=/usr/local/lib/libjemalloc.so.2

ENV LD_PRELOAD=/usr/lib/x86_64-linux-gnu/libjemalloc.so.2

ADD build/libs/thumbnail-extraction-worker-0.0.1-SNAPSHOT.jar worker-app.jar
ENTRYPOINT ["java", \
    "-XX:+HeapDumpOnOutOfMemoryError", \
    "-XX:HeapDumpPath=/var/log/app/heapdump.hprof", \
    "-Djava.net.preferIPv6Addresses=true", \
    "-jar", "worker-app.jar"]
