package com.orv.media.service;

import com.orv.media.infrastructure.AudioCompressor;
import com.orv.media.infrastructure.AudioExtractor;
import com.orv.media.repository.AudioStorage;
import com.orv.media.repository.InterviewAudioRecordingRepository;
import com.orv.media.domain.InterviewAudioRecording;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@Slf4j
public class AudioService {

    private final AudioExtractor audioExtractor;
    private final AudioCompressor audioCompressor;
    private final InterviewAudioRecordingRepository audioRecordingRepository;
    private final AudioStorage audioStorage;

    public AudioService(AudioExtractor audioExtractor,
                        AudioCompressor audioCompressor,
                        InterviewAudioRecordingRepository audioRecordingRepository,
                        AudioStorage audioStorage) {
        this.audioExtractor = audioExtractor;
        this.audioCompressor = audioCompressor;
        this.audioRecordingRepository = audioRecordingRepository;
        this.audioStorage = audioStorage;
    }

    public InterviewAudioRecording extractAndSaveAudioFromVideo(
            File videoFile, UUID storyboardId, UUID memberId) throws IOException {

        File tempAudioExtractedFile = null;
        File tempAudioCompressedFile = null;

        try {
            tempAudioExtractedFile = createTempFile("extracted_audio_", ".wav");
            int durationSeconds = audioExtractor.extractAudio(videoFile, tempAudioExtractedFile, "wav");

            tempAudioCompressedFile = createTempFile("compressed_audio_", ".opus");
            audioCompressor.compress(tempAudioExtractedFile, tempAudioCompressedFile);

            UUID fileId;
            try (FileInputStream fis = new FileInputStream(tempAudioCompressedFile)) {
                fileId = audioStorage.save(fis, "audio/ogg; codecs=opus", tempAudioCompressedFile.length());
            }
            String audioUrl = audioStorage.getUrl(fileId);
            log.info("Uploaded audio to S3: {}", audioUrl);

            try {
                InterviewAudioRecording audioRecording = InterviewAudioRecording.builder()
                        .id(UUID.randomUUID())
                        .storyboardId(storyboardId)
                        .memberId(memberId)
                        .audioUrl(audioUrl)
                        .createdAt(OffsetDateTime.now())
                        .runningTime(durationSeconds)
                        .build();

                InterviewAudioRecording saved = audioRecordingRepository.save(audioRecording);
                log.info("Saved audio metadata to database: {}", saved.getId());
                return saved;
            } catch (Exception dbException) {
                // DB 저장 실패 시 S3 파일 삭제 (보상 트랜잭션)
                try {
                    audioStorage.delete(fileId);
                    log.info("Compensated S3 audio upload by deleting file: {}", fileId);
                } catch (Exception deleteException) {
                    log.error("Failed to compensate S3 upload. Manual cleanup required for file: {}", fileId, deleteException);
                }
                throw dbException;
            }

        } finally {
            cleanupTempFile(tempAudioExtractedFile);
            cleanupTempFile(tempAudioCompressedFile);
        }
    }

    public void deleteAudio(UUID audioRecordingId, String audioUrl) {
        try {
            audioRecordingRepository.delete(audioRecordingId);
            log.info("Deleted audio recording from DB: {}", audioRecordingId);

            UUID fileId = extractFileIdFromUrl(audioUrl);
            audioStorage.delete(fileId);
            log.info("Deleted audio file from S3: {}", fileId);

            log.info("Successfully deleted audio recording {} and S3 file {}", audioRecordingId, audioUrl);
        } catch (Exception e) {
            log.error("Failed to delete audio recording: {}. Manual cleanup may be required.", audioRecordingId, e);
        }
    }

    private UUID extractFileIdFromUrl(String audioUrl) {
        try {
            String path = audioUrl.split("\\?")[0];
            String[] parts = path.split("/");
            String fileIdString = parts[parts.length - 1];

            return UUID.fromString(fileIdString);
        } catch (Exception e) {
            log.error("Failed to extract file ID from URL: {}", audioUrl, e);
            throw new IllegalArgumentException("Invalid audio URL format: " + audioUrl, e);
        }
    }

    private File createTempFile(String prefix, String suffix) throws IOException {
        File tempFile = File.createTempFile(prefix, suffix);
        tempFile.deleteOnExit();
        return tempFile;
    }

    private void cleanupTempFile(File file) {
        if (file != null && file.exists()) {
            file.delete();
        }
    }
}