package com.orv.worker.thumbnailextraction;

import java.time.Duration;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import com.orv.archive.domain.VideoThumbnailExtractionJob;
import com.orv.archive.repository.VideoThumbnailExtractionJobRepository;
import com.orv.worker.thumbnailextraction.service.ThumbnailExtractionJobService;

import lombok.extern.slf4j.Slf4j;

@Component
@EnableScheduling
@Slf4j
public class VideoThumbnailExtractionWorker {

    private final VideoThumbnailExtractionJobRepository jobRepository;
    private final ThumbnailExtractionJobService jobService;
    private final ThreadPoolTaskExecutor executor;
    private final Duration stuckThreshold;

    public VideoThumbnailExtractionWorker(
            VideoThumbnailExtractionJobRepository jobRepository,
            ThumbnailExtractionJobService jobService,
            @Qualifier("thumbnailExtractionExecutor") ThreadPoolTaskExecutor executor,
            @Value("${worker.thumbnail-extraction.stuck-threshold-minutes:10}") int stuckThresholdMinutes) {
        this.jobRepository = jobRepository;
        this.jobService = jobService;
        this.executor = executor;
        this.stuckThreshold = Duration.ofMinutes(stuckThresholdMinutes);
    }

    @Scheduled(fixedDelayString = "${worker.thumbnail-extraction.poll-interval-ms:1000}")
    public void poll() {
        while (hasAvailableThread()) {
            Optional<VideoThumbnailExtractionJob> jobOpt = jobRepository.claimNext(stuckThreshold);

            if (jobOpt.isEmpty()) {
                return;
            }

            VideoThumbnailExtractionJob job = jobOpt.get();
            log.info("Dispatching thumbnail job #{} for video {}", job.getId(), job.getVideoId());

            try {
                executor.execute(() -> jobService.processJob(job));
            } catch (org.springframework.core.task.TaskRejectedException e) {
                log.warn("Thread pool full, will retry thumbnail job #{} next poll cycle", job.getId());
                return;
            }
        }
    }

    private boolean hasAvailableThread() {
        return executor.getActiveCount() < executor.getMaxPoolSize();
    }
}
