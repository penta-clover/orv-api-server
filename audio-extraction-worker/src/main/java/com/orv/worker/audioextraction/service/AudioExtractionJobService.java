package com.orv.worker.audioextraction.service;

import java.io.File;

import org.springframework.stereotype.Service;

import com.orv.media.domain.AudioExtractionJob;
import com.orv.media.domain.InterviewAudioRecording;
import com.orv.media.repository.AudioExtractionJobRepository;
import com.orv.media.service.AudioService;
import com.orv.archive.service.infrastructure.VideoDownloader;
import com.orv.recap.repository.RecapReservationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class AudioExtractionJobService {

    private final AudioExtractionJobRepository jobRepository;
    private final VideoDownloader videoDownloader;
    private final AudioService audioService;
    private final RecapReservationRepository recapReservationRepository;

    public void processJob(AudioExtractionJob job) {
        String threadName = Thread.currentThread().getName();
        log.info("[{}] Processing audio extraction job #{} for video {}", threadName, job.getId(), job.getVideoId());

        File videoFile = null;
        try {
            // 1. Download video to temp file
            videoFile = videoDownloader.download(job.getVideoId());

            // 2. Extract and save audio (FFmpeg + Opus + S3 upload)
            InterviewAudioRecording audioRecording = audioService.extractAndSaveAudioFromVideo(
                    videoFile,
                    job.getStoryboardId(),
                    job.getMemberId()
            );

            // 3. Mark job completed with result
            jobRepository.markCompleted(job.getId(), audioRecording.getId());
            log.info("[{}] Completed audio extraction job #{}, audioRecordingId={}", threadName, job.getId(), audioRecording.getId());

            // 4. Link audio to recap reservation (best-effort)
            if (job.getRecapReservationId() != null) {
                try {
                    recapReservationRepository.linkAudioRecording(job.getRecapReservationId(), audioRecording.getId());
                    log.info("[{}] Linked audio {} to recap reservation {}", threadName, audioRecording.getId(), job.getRecapReservationId());
                } catch (Exception e) {
                    log.error("[{}] Failed to link audio {} to recap reservation {}. Manual linking may be required.",
                            threadName, audioRecording.getId(), job.getRecapReservationId(), e);
                }
            }

        } catch (Exception e) {
            log.error("[{}] Failed to process audio extraction job #{}", threadName, job.getId(), e);
            jobRepository.markFailed(job.getId());
        } finally {
            videoDownloader.deleteSafely(videoFile);
        }
    }
}
