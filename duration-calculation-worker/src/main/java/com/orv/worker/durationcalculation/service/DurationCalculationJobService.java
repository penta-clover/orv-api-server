package com.orv.worker.durationcalculation.service;

import java.io.File;

import org.springframework.stereotype.Service;

import com.orv.archive.domain.DurationCalculationResult;
import com.orv.archive.domain.VideoDurationCalculationJob;
import com.orv.archive.repository.VideoDurationCalculationJobRepository;
import com.orv.archive.repository.VideoRepository;
import com.orv.archive.service.infrastructure.VideoDurationCalculator;
import com.orv.archive.service.infrastructure.VideoDownloader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class DurationCalculationJobService {

    private final VideoDurationCalculationJobRepository jobRepository;
    private final VideoRepository videoRepository;
    private final VideoDurationCalculator durationCalculator;
    private final VideoDownloader videoDownloader;

    public void processJob(VideoDurationCalculationJob job) {
        String threadName = Thread.currentThread().getName();
        log.info("[{}] Processing job #{} for video {}", threadName, job.getId(), job.getVideoId());

        File videoFile = null;
        try {
            videoFile = videoDownloader.download(job.getVideoId());

            DurationCalculationResult calculationResult = durationCalculator.calculate(videoFile);

            if (!calculationResult.success()) {
                handleCalculationFailure(job, calculationResult.errorMessage());
                return;
            }

            boolean isUpdated = videoRepository.updateRunningTime(job.getVideoId(), calculationResult.durationSeconds());

            if (!isUpdated) {
                handleUpdateFailure(job);
                return;
            }

            jobRepository.markCompleted(job.getId());
            log.info("[{}] Completed job #{} - duration: {}s", threadName, job.getId(), calculationResult.durationSeconds());
        } catch (Exception e) {
            log.error("[{}] Failed to process job #{}", threadName, job.getId(), e);
            jobRepository.markFailed(job.getId());
        } finally {
            videoDownloader.deleteSafely(videoFile);
        }
    }

    private void handleCalculationFailure(VideoDurationCalculationJob job, String errorMessage) {
        log.warn("Job #{} failed: {}", job.getId(), errorMessage);
        jobRepository.markFailed(job.getId());
    }

    private void handleUpdateFailure(VideoDurationCalculationJob job) {
        log.error("Failed to update running_time for job #{}", job.getId());
        jobRepository.markFailed(job.getId());
    }
}
