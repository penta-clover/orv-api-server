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
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.Duration;
import java.util.Optional;

@Component
@EnableScheduling
@Slf4j
public class VideoDurationCalculationWorker {

    private final VideoDurationCalculationJobRepository jobRepository;
    private final VideoRepository videoRepository;
    private final VideoDurationCalculator calculator;
    private final VideoDownloader downloader;
    private final ThreadPoolTaskExecutor executor;
    private final Duration stuckThreshold;

    public VideoDurationCalculationWorker(
            VideoDurationCalculationJobRepository jobRepository,
            VideoRepository videoRepository,
            VideoDurationCalculator calculator,
            VideoDownloader downloader,
            @Qualifier("durationCalculationExecutor") ThreadPoolTaskExecutor executor,
            @Value("${worker.duration-calculation.stuck-threshold-minutes:10}") int stuckThresholdMinutes) {
        this.jobRepository = jobRepository;
        this.videoRepository = videoRepository;
        this.calculator = calculator;
        this.downloader = downloader;
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
            log.info("Dispatching job #{} for video {}", job.getId(), job.getVideoId());

            try {
                executor.execute(() -> processJob(job));
            } catch (org.springframework.core.task.TaskRejectedException e) {
                log.warn("Thread pool full, will retry job #{} next poll cycle", job.getId());
                return;
            }
        }
    }

    private void processJob(VideoDurationCalculationJob job) {
        long startTime = System.currentTimeMillis();
        String threadName = Thread.currentThread().getName();
        log.info("[{}] Processing job #{} for video {}", threadName, job.getId(), job.getVideoId());

        File tempFile = null;
        try {
            long downloadStartTime = System.currentTimeMillis();
            tempFile = downloader.download(job.getVideoId());
            long downloadTime = System.currentTimeMillis() - downloadStartTime;

            long processStartTime = System.currentTimeMillis();
            DurationCalculationResult result = calculator.calculate(tempFile);
            long processTime = System.currentTimeMillis() - processStartTime;

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

            long totalElapsed = System.currentTimeMillis() - startTime;
            log.info("[{}] Completed job #{} in {}ms (download: {}ms, process: {}ms) - duration: {}s",
                    threadName, job.getId(), totalElapsed, downloadTime, processTime, result.durationSeconds());

        } catch (Exception e) {
            log.error("[{}] Failed to process job #{}", threadName, job.getId(), e);
            jobRepository.markFailed(job.getId());
        } finally {
            downloader.deleteSafely(tempFile);
        }
    }

    private boolean hasAvailableThread() {
        return executor.getActiveCount() < executor.getMaxPoolSize();
    }
}
