package com.orv.worker.thumbnailextraction.service;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Service;

import com.orv.archive.domain.CandidateThumbnailExtractionResult;
import com.orv.archive.domain.CandidateThumbnailExtractionResult.CandidateFrame;
import com.orv.archive.domain.InputStreamWithMetadata;
import com.orv.archive.domain.ThumbnailCandidate;
import com.orv.archive.domain.VideoThumbnailExtractionJob;
import com.orv.archive.repository.ThumbnailCandidateRepository;
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
    private final ThumbnailCandidateRepository candidateRepository;
    private final VideoThumbnailExtractor thumbnailExtractor;
    private final VideoDownloader videoDownloader;

    public void processJob(VideoThumbnailExtractionJob job) {
        String threadName = Thread.currentThread().getName();
        log.info("[{}] Processing thumbnail job #{} for video {}", threadName, job.getId(), job.getVideoId());

        File videoFile = null;
        try {
            candidateRepository.deleteByJobId(job.getId());

            videoFile = videoDownloader.download(job.getVideoId());

            CandidateThumbnailExtractionResult result = thumbnailExtractor.extractCandidates(videoFile);
            if (!result.success()) {
                log.warn("Thumbnail job #{} failed: {}", job.getId(), result.errorMessage());
                jobRepository.markFailed(job.getId());
                return;
            }

            long uploadStart = System.nanoTime();
            uploadAndSaveCandidates(job, result.candidates());
            long uploadMs = (System.nanoTime() - uploadStart) / 1_000_000;
            log.info("perf operation=upload-and-save job_id={} duration_ms={} candidates={}",
                    job.getId(), uploadMs, result.candidates().size());

            jobRepository.markCompleted(job.getId());
            log.info("[{}] Completed thumbnail job #{} ({} candidates saved)",
                    threadName, job.getId(), result.candidates().size());

        } catch (Exception e) {
            log.error("[{}] Failed to process thumbnail job #{}", threadName, job.getId(), e);
            jobRepository.markFailed(job.getId());
        } finally {
            videoDownloader.deleteSafely(videoFile);
        }
    }

    private void uploadAndSaveCandidates(
            VideoThumbnailExtractionJob job, List<CandidateFrame> candidates) throws IOException {
        for (CandidateFrame candidate : candidates) {
            InputStreamWithMetadata stream =
                    VideoProcessingUtils.bufferedImageToInputStream(candidate.image(), THUMBNAIL_FORMAT);

            String fileKey = videoRepository.uploadThumbnailCandidate(
                    stream.getThumbnailImage(), stream.getMetadata());

            saveCandidateRecord(job, candidate, fileKey);

            log.debug("Saved candidate: timestamp={}ms, sharpness={}, fileKey={}",
                    candidate.timestampMs(), candidate.sharpnessScore(), fileKey);
        }
    }

    private void saveCandidateRecord(
            VideoThumbnailExtractionJob job, CandidateFrame candidate, String fileKey) {
        ThumbnailCandidate entity = new ThumbnailCandidate();
        entity.setJobId(job.getId());
        entity.setVideoId(job.getVideoId());
        entity.setTimestampMs(candidate.timestampMs());
        entity.setFileKey(fileKey);
        candidateRepository.save(entity);
    }
}
