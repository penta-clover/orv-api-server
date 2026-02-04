package com.orv.worker.thumbnailextraction.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ThumbnailExtractionWorkerConfig {

    @Value("${worker.thumbnail-extraction.threads:2}")
    private int threadCount;

    @Bean(name = "thumbnailExtractionExecutor")
    public ThreadPoolTaskExecutor thumbnailExtractionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threadCount);
        executor.setMaxPoolSize(threadCount);
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("thumbnail-worker-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
