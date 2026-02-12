package com.orv.worker.audioextraction.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AudioExtractionWorkerConfig {

    @Value("${worker.audio-extraction.threads:2}")
    private int threadCount;

    @Bean(name = "audioExtractionExecutor")
    public ThreadPoolTaskExecutor audioExtractionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threadCount);
        executor.setMaxPoolSize(threadCount);
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("audio-worker-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
