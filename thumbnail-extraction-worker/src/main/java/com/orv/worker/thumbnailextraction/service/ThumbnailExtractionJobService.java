package com.orv.worker.thumbnailextraction.service;

import java.io.File;

import org.springframework.stereotype.Service;

import com.orv.archive.domain.InputStreamWithMetadata;
import com.orv.archive.domain.ThumbnailExtractionResult;
import com.orv.archive.domain.VideoThumbnailExtractionJob;
import com.orv.archive.repository.VideoRepository;
import com.orv.archive.repository.VideoThumbnailExtractionJobRepository;
import com.orv.archive.service.VideoProcessingUtils;
import com.orv.archive.service.infrastructure.VideoDownloader;
import com.orv.archive.service.infrastructure.VideoThumbnailExtractor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ThumbnailExtractionJobService {

    private static final String THUMBNAIL_FORMAT = "jpg";

    private final VideoThumbnailExtractionJobRepository jobRepository;
    private final VideoRepository videoRepository;
    private final VideoThumbnailExtractor thumbnailExtractor;
    private final VideoDownloader videoDownloader;

    public void processJob(VideoThumbnailExtractionJob job) {
        String threadName = Thread.currentThread().getName();
        log.info("[{}] Processing thumbnail job #{} for video {}", threadName, job.getId(), job.getVideoId());

        File videoFile = null;
        try {
            videoFile = videoDownloader.download(job.getVideoId());

            ThumbnailExtractionResult result = thumbnailExtractor.extract(videoFile);

            if (!result.success()) {
                handleExtractionFailure(job, result);
                return;
            }

            InputStreamWithMetadata thumbnailStream =
                VideoProcessingUtils.bufferedImageToInputStream(result.thumbnail(), THUMBNAIL_FORMAT);

            boolean isUpdated = videoRepository.updateThumbnail(
                job.getVideoId(),
                thumbnailStream.getThumbnailImage(),
                thumbnailStream.getMetadata()
            );

            if (!isUpdated) {
                handleUpdateFailure(job);
                return;
            }

            jobRepository.markCompleted(job.getId());
            log.info("[{}] Completed thumbnail job #{}", threadName, job.getId());

        } catch (Exception e) {
            log.error("[{}] Failed to process thumbnail job #{}", threadName, job.getId(), e);
            jobRepository.markFailed(job.getId());
        } finally {
            videoDownloader.deleteSafely(videoFile);
        }
    }

    private void handleUpdateFailure(VideoThumbnailExtractionJob job) {
        log.error("Failed to update thumbnail for job #{}", job.getId());
        jobRepository.markFailed(job.getId());
    }

    private void handleExtractionFailure(VideoThumbnailExtractionJob job, ThumbnailExtractionResult result) {
        log.warn("Thumbnail job #{} failed: {}", job.getId(), result.errorMessage());
        jobRepository.markFailed(job.getId());
    }
}
