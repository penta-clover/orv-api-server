package com.orv.worker.thumbnailextraction;

import com.orv.archive.domain.InputStreamWithMetadata;
import com.orv.archive.domain.ThumbnailExtractionResult;
import com.orv.archive.domain.VideoThumbnailExtractionJob;
import com.orv.archive.repository.VideoRepository;
import com.orv.archive.repository.VideoThumbnailExtractionJobRepository;
import com.orv.archive.service.VideoProcessingUtils;
import com.orv.archive.service.infrastructure.VideoDownloader;
import com.orv.archive.service.infrastructure.VideoThumbnailExtractor;
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
public class VideoThumbnailExtractionWorker {

    private final VideoThumbnailExtractionJobRepository jobRepository;
    private final VideoRepository videoRepository;
    private final VideoThumbnailExtractor extractor;
    private final VideoDownloader downloader;
    private final ThreadPoolTaskExecutor executor;
    private final Duration stuckThreshold;

    public VideoThumbnailExtractionWorker(
            VideoThumbnailExtractionJobRepository jobRepository,
            VideoRepository videoRepository,
            VideoThumbnailExtractor extractor,
            VideoDownloader downloader,
            @Qualifier("thumbnailExtractionExecutor") ThreadPoolTaskExecutor executor,
            @Value("${worker.thumbnail-extraction.stuck-threshold-minutes:10}") int stuckThresholdMinutes) {
        this.jobRepository = jobRepository;
        this.videoRepository = videoRepository;
        this.extractor = extractor;
        this.downloader = downloader;
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
                executor.execute(() -> processJob(job));
            } catch (org.springframework.core.task.TaskRejectedException e) {
                log.warn("Thread pool full, will retry thumbnail job #{} next poll cycle", job.getId());
                return;
            }
        }
    }

    private void processJob(VideoThumbnailExtractionJob job) {
        long startTime = System.currentTimeMillis();
        String threadName = Thread.currentThread().getName();
        log.info("[{}] Processing thumbnail job #{} for video {}", threadName, job.getId(), job.getVideoId());

        File tempFile = null;
        try {
            tempFile = downloader.download(job.getVideoId());

            ThumbnailExtractionResult result = extractor.extract(tempFile);
            if (!result.success()) {
                log.warn("Thumbnail job #{} failed: {}", job.getId(), result.errorMessage());
                jobRepository.markFailed(job.getId());
                return;
            }

            InputStreamWithMetadata thumbnailStream =
                VideoProcessingUtils.bufferedImageToInputStream(result.thumbnail(), "jpg");

            boolean updated = videoRepository.updateThumbnail(
                job.getVideoId(),
                thumbnailStream.getThumbnailImage(),
                thumbnailStream.getMetadata()
            );

            if (!updated) {
                log.error("Failed to update thumbnail for job #{}", job.getId());
                jobRepository.markFailed(job.getId());
                return;
            }

            jobRepository.markCompleted(job.getId());

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("[{}] Completed thumbnail job #{} in {}ms", threadName, job.getId(), elapsed);

        } catch (Exception e) {
            log.error("[{}] Failed to process thumbnail job #{}", threadName, job.getId(), e);
            jobRepository.markFailed(job.getId());
        } finally {
            downloader.deleteSafely(tempFile);
        }
    }

    private boolean hasAvailableThread() {
        return executor.getActiveCount() < executor.getMaxPoolSize();
    }
}
