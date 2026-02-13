FROM eclipse-temurin:21-jre

ARG JEMALLOC_PROFILING=false

# Install FFmpeg and jemalloc
RUN apt-get update && \
    apt-get install -y ffmpeg && \
    if [ "$JEMALLOC_PROFILING" = "true" ]; then \
        apt-get install -y build-essential autoconf git libunwind-dev && \
        git clone --depth 1 --branch 5.3.0 https://github.com/jemalloc/jemalloc.git /tmp/jemalloc && \
        cd /tmp/jemalloc && \
        autoconf && \
        ./configure --enable-prof --prefix=/usr/local && \
        make -j$(nproc) && \
        make install && \
        ldconfig && \
        rm -rf /tmp/jemalloc && \
        apt-get purge -y build-essential autoconf git && \
        apt-get autoremove -y; \
    else \
        apt-get install -y libjemalloc2 && \
        ln -s /usr/lib/$(dpkg --print-architecture)-linux-gnu/libjemalloc.so.2 /usr/local/lib/libjemalloc.so.2; \
    fi && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

ENV LD_PRELOAD=/usr/local/lib/libjemalloc.so.2

ARG MALLOC_CONF_VALUE=""
ENV MALLOC_CONF=${MALLOC_CONF_VALUE}

RUN mkdir -p /var/log/app

ADD build/libs/thumbnail-extraction-worker-0.0.1-SNAPSHOT.jar worker-app.jar
ENTRYPOINT ["java", \
    "-XX:+HeapDumpOnOutOfMemoryError", \
    "-XX:HeapDumpPath=/var/log/app/heapdump.hprof", \
    "-Djava.net.preferIPv6Addresses=true", \
    "-jar", "worker-app.jar"]
