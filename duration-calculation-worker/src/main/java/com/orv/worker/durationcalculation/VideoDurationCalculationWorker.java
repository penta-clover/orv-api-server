package com.orv.worker.durationcalculation;

import java.time.Duration;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import com.orv.archive.domain.VideoDurationCalculationJob;
import com.orv.archive.repository.VideoDurationCalculationJobRepository;
import com.orv.worker.durationcalculation.service.DurationCalculationJobService;

import lombok.extern.slf4j.Slf4j;

@Component
@EnableScheduling
@Slf4j
public class VideoDurationCalculationWorker {

    private final VideoDurationCalculationJobRepository jobRepository;
    private final DurationCalculationJobService jobService;
    private final ThreadPoolTaskExecutor executor;
    private final Duration stuckThreshold;

    public VideoDurationCalculationWorker(
            VideoDurationCalculationJobRepository jobRepository,
            DurationCalculationJobService jobService,
            @Qualifier("durationCalculationExecutor") ThreadPoolTaskExecutor executor,
            @Value("${worker.duration-calculation.stuck-threshold-minutes:10}") int stuckThresholdMinutes) {
        this.jobRepository = jobRepository;
        this.jobService = jobService;
        this.executor = executor;
        this.stuckThreshold = Duration.ofMinutes(stuckThresholdMinutes);
    }

    @Scheduled(fixedDelayString = "${worker.duration-calculation.poll-interval-ms:1000}")
    public void poll() {
        while (hasAvailableThread()) {
            Optional<VideoDurationCalculationJob> jobOpt = jobRepository.claimNext(stuckThreshold);

            if (jobOpt.isEmpty()) {
                return;
            }

            VideoDurationCalculationJob job = jobOpt.get();
            log.info("Dispatching duration calculation job #{} for video {}", job.getId(), job.getVideoId());

            try {
                executor.execute(() -> jobService.processJob(job));
            } catch (org.springframework.core.task.TaskRejectedException e) {
                log.warn("Thread pool full, resetting duration calculation job #{} to PENDING", job.getId());
                jobRepository.resetToPending(job.getId());
                return;
            }
        }
    }

    private boolean hasAvailableThread() {
        return executor.getActiveCount() < executor.getMaxPoolSize();
    }
}
