package com.orv.worker.durationcalculation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@ConditionalOnProperty(name = "worker.duration-calculation.enabled", havingValue = "true")
public class WorkerThreadPoolConfig {

    @Value("${worker.duration-calculation.threads:2}")
    private int threadCount;

    @Bean(name = "durationCalculationExecutor")
    public ThreadPoolTaskExecutor durationCalculationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threadCount);
        executor.setMaxPoolSize(threadCount);
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("duration-worker-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
