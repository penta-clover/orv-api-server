package com.orv.api.domain.media.service;

import com.orv.api.domain.archive.repository.AudioRepository;
import com.orv.api.domain.archive.service.dto.AudioMetadata;
import com.orv.api.domain.media.infrastructure.AudioCompressor;
import com.orv.api.domain.media.infrastructure.AudioExtractor;
import com.orv.api.domain.media.repository.InterviewAudioRecordingRepository;
import com.orv.api.domain.media.service.dto.InterviewAudioRecording;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@Slf4j
public class AudioService {

    private final AudioExtractor audioExtractor;
    private final AudioCompressor audioCompressor;
    private final InterviewAudioRecordingRepository audioRecordingRepository;
    private final AudioRepository audioRepository;

    public AudioService(AudioExtractor audioExtractor,
                        AudioCompressor audioCompressor,
                        InterviewAudioRecordingRepository audioRecordingRepository,
                        AudioRepository audioRepository) {
        this.audioExtractor = audioExtractor;
        this.audioCompressor = audioCompressor;
        this.audioRecordingRepository = audioRecordingRepository;
        this.audioRepository = audioRepository;
    }

    public InterviewAudioRecording extractAndSaveAudioFromVideo(
            InputStream videoStream, UUID storyboardId, UUID memberId, String title, Integer runningTime) throws IOException {
        
        File tempVideoFile = null;
        File tempAudioExtractedFile = null;
        File tempAudioCompressedFile = null;

        try {
            tempVideoFile = convertStreamToFile(videoStream);

            tempAudioExtractedFile = createTempFile("extracted_audio_", ".wav");
            audioExtractor.extractAudio(tempVideoFile, tempAudioExtractedFile, "wav");

            tempAudioCompressedFile = createTempFile("compressed_audio_", ".opus");
            audioCompressor.compress(tempAudioExtractedFile, tempAudioCompressedFile);

            URI resourceUrl = uploadAudioToS3(tempAudioCompressedFile, storyboardId, memberId, title, runningTime);

            return saveAudioMetadata(resourceUrl, storyboardId, memberId, runningTime);

        } finally {
            cleanupTempFile(tempVideoFile);
            cleanupTempFile(tempAudioExtractedFile);
            cleanupTempFile(tempAudioCompressedFile);
        }
    }

    private File convertStreamToFile(InputStream videoStream) throws IOException {
        Path tempPath = Files.createTempFile("recap_video_", ".mp4");
        Files.copy(videoStream, tempPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        File tempFile = tempPath.toFile();
        log.info("Video downloaded to temporary file: {}", tempFile.getAbsolutePath());
        return tempFile;
    }

    private File createTempFile(String prefix, String suffix) throws IOException {
        return Files.createTempFile(prefix, suffix).toFile();
    }

    private URI uploadAudioToS3(File audioFile, UUID storyboardId, UUID memberId, String title, Integer runningTime) throws IOException {
        try (InputStream inputStream = new FileInputStream(audioFile)) {
            AudioMetadata audioMetadata = new AudioMetadata(
                    storyboardId,
                    memberId,
                    title,
                    "audio/opus",
                    runningTime != null ? runningTime : 0,
                    audioFile.length()
            );
            return audioRepository.save(inputStream, audioMetadata)
                    .orElseThrow(() -> new IOException("S3에 오디오 파일 업로드 실패"));
        }
    }

    private InterviewAudioRecording saveAudioMetadata(URI resourceUrl, UUID storyboardId, UUID memberId, Integer runningTime) {
        InterviewAudioRecording audioRecording = InterviewAudioRecording.builder()
                .id(UUID.randomUUID())
                .storyboardId(storyboardId)
                .memberId(memberId)
                .audioUrl(resourceUrl.toString())
                .createdAt(OffsetDateTime.now())
                .runningTime(runningTime != null ? runningTime : 0)
                .build();

        return audioRecordingRepository.save(audioRecording);
    }

    private void cleanupTempFile(File file) {
        if (file != null && file.exists()) {
            try {
                Files.deleteIfExists(file.toPath());
                log.info("임시 파일 삭제: {}", file.getAbsolutePath());
            } catch (IOException e) {
                log.warn("임시 파일 삭제 실패: {}", file.getAbsolutePath(), e);
            }
        }
    }
}
