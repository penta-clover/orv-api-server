package com.orv.worker.durationcalculation;

import com.orv.archive.service.infrastructure.VideoDurationCalculator;
import com.orv.archive.service.infrastructure.VideoDownloader;
import com.orv.archive.repository.VideoDurationCalculationJobRepository;
import com.orv.archive.repository.VideoRepository;
import com.orv.archive.domain.DurationCalculationResult;
import com.orv.archive.domain.VideoDurationCalculationJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.io.File;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@ConditionalOnProperty(name = "worker.duration-calculation.enabled", havingValue = "true")
@Slf4j
public class VideoDurationCalculationWorker implements CommandLineRunner {

    private final VideoDurationCalculationJobRepository jobRepository;
    private final VideoRepository videoRepository;
    private final VideoDurationCalculator calculator;
    private final VideoDownloader downloader;
    private final ThreadPoolTaskExecutor executor;

    @Value("${worker.duration-calculation.poll-interval-ms:3000}")
    private long pollIntervalMs;

    @Value("${worker.duration-calculation.max-backoff-ms:30000}")
    private long maxBackoffMs;

    @Value("${worker.duration-calculation.stuck-threshold-minutes:10}")
    private int stuckThresholdMinutes;

    private final AtomicBoolean running = new AtomicBoolean(true);

    public VideoDurationCalculationWorker(
            VideoDurationCalculationJobRepository jobRepository,
            VideoRepository videoRepository,
            VideoDurationCalculator calculator,
            VideoDownloader downloader,
            @Qualifier("durationCalculationExecutor") ThreadPoolTaskExecutor executor) {
        this.jobRepository = jobRepository;
        this.videoRepository = videoRepository;
        this.calculator = calculator;
        this.downloader = downloader;
        this.executor = executor;
    }

    @Override
    public void run(String... args) {
        int threads = executor.getCorePoolSize();
        log.info("Starting VideoDurationCalculationWorker with {} threads", threads);

        Duration stuckThreshold = Duration.ofMinutes(stuckThresholdMinutes);

        for (int i = 0; i < threads; i++) {
            int workerId = i;
            executor.execute(() -> workerLoop(workerId, stuckThreshold));
        }

        log.info("All {} worker threads started", threads);
    }

    private void workerLoop(int workerId, Duration stuckThreshold) {
        log.info("Worker-{} started", workerId);
        long backoff = pollIntervalMs;

        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                Optional<VideoDurationCalculationJob> jobOpt = jobRepository.claimNext(stuckThreshold);

                if (jobOpt.isEmpty()) {
                    Thread.sleep(backoff);
                    backoff = Math.min(backoff * 2, maxBackoffMs);
                    continue;
                }

                backoff = pollIntervalMs;
                processJob(jobOpt.get(), workerId);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Worker-{} unexpected error", workerId, e);
            }
        }

        log.info("Worker-{} stopped", workerId);
    }

    private void processJob(VideoDurationCalculationJob job, int workerId) {
        long startTime = System.currentTimeMillis();
        log.info("Worker-{} processing job #{} for video {}", workerId, job.getId(), job.getVideoId());

        File tempFile = null;
        try {
            tempFile = downloader.download(job.getVideoId());

            DurationCalculationResult result = calculator.calculate(tempFile);
            if (!result.success()) {
                log.warn("Job #{} failed: {}", job.getId(), result.errorMessage());
                jobRepository.markFailed(job.getId());
                return;
            }

            boolean updated = videoRepository.updateRunningTime(job.getVideoId(), result.durationSeconds());
            if (!updated) {
                log.error("Failed to update running_time for job #{}", job.getId());
                jobRepository.markFailed(job.getId());
                return;
            }

            jobRepository.markCompleted(job.getId());

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Worker-{} completed job #{} in {}ms - duration: {}s",
                    workerId, job.getId(), elapsed, result.durationSeconds());

        } catch (Exception e) {
            log.error("Worker-{} failed to process job #{}", workerId, job.getId(), e);
            jobRepository.markFailed(job.getId());
        } finally {
            downloader.deleteSafely(tempFile);
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down VideoDurationCalculationWorker");
        running.set(false);
    }
}
