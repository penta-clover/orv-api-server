package com.orv.api.domain.media.service;

import com.orv.api.domain.archive.repository.AudioRepository;
import com.orv.api.domain.archive.service.dto.AudioMetadata;
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
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class AudioService {

    private final AudioExtractService audioExtractService;
    private final AudioCompressionService audioCompressionService;
    private final InterviewAudioRecordingRepository audioRecordingRepository;
    private final AudioRepository audioRepository;

    public AudioService(AudioExtractService audioExtractService,
                        AudioCompressionService audioCompressionService,
                        InterviewAudioRecordingRepository audioRecordingRepository,
                        AudioRepository audioRepository) {
        this.audioExtractService = audioExtractService;
        this.audioCompressionService = audioCompressionService;
        this.audioRecordingRepository = audioRecordingRepository;
        this.audioRepository = audioRepository;
    }

    public InterviewAudioRecording processAndSaveAudio(
            File inputVideoFile, UUID storyboardId, UUID memberId, String title) throws IOException {

        // 1. 임시 파일 경로 설정
        Path tempAudioExtractedPath = null;
        Path tempAudioCompressedPath = null;
        File tempAudioExtractedFile = null;
        File tempAudioCompressedFile = null;

        try {
            tempAudioExtractedPath = Files.createTempFile("extracted_audio_", ".wav"); // 추출된 오디오 (임시)
            tempAudioCompressedPath = Files.createTempFile("compressed_audio_", ".opus"); // 압축된 오디오 (임시)
            tempAudioExtractedFile = tempAudioExtractedPath.toFile();
            tempAudioCompressedFile = tempAudioCompressedPath.toFile();

            // 2. 오디오 추출 (비디오 -> WAV)
            log.info("비디오에서 오디오 추출 시작: {}", inputVideoFile.getAbsolutePath());
            audioExtractService.extractAudio(inputVideoFile, tempAudioExtractedFile, "wav");
            log.info("오디오 추출 완료: {}", tempAudioExtractedFile.getAbsolutePath());

            // 3. 오디오 압축 (WAV -> Opus)
            log.info("오디오 압축 시작: {}", tempAudioExtractedFile.getAbsolutePath());
            audioCompressionService.compress(tempAudioExtractedFile, tempAudioCompressedFile);
            log.info("오디오 압축 완료: {}", tempAudioCompressedFile.getAbsolutePath());

            // 4. S3에 업로드
            // TODO: running_time 계산 (FFmpeg 등으로 정확한 시간 획득 필요)
            int runningTime = 60; // 현재는 임시값 (예: 60초)
            URI resourceUrl;
            try (InputStream inputStream = new FileInputStream(tempAudioCompressedFile)) {
                AudioMetadata audioMetadata = new AudioMetadata(
                        storyboardId,
                        memberId,
                        title,
                        "audio/opus",
                        runningTime,
                        tempAudioCompressedFile.length()
                );
                Optional<URI> urlOptional = audioRepository.save(inputStream, audioMetadata);
                resourceUrl = urlOptional.orElseThrow(() -> new IOException("S3에 오디오 파일 업로드 실패"));
            }

            // 5. 메타데이터 저장
            InterviewAudioRecording audioRecording = InterviewAudioRecording.builder()
                    .id(UUID.randomUUID())
                    .storyboardId(storyboardId)
                    .memberId(memberId)
                    .audioUrl(resourceUrl.toString())
                    .createdAt(OffsetDateTime.now())
                    .runningTime(runningTime)
                    .build();

            return audioRecordingRepository.save(audioRecording);

        } finally {
            // 임시 파일 정리
            if (tempAudioExtractedFile != null && tempAudioExtractedFile.exists()) {
                Files.deleteIfExists(tempAudioExtractedPath);
                log.info("임시 추출 오디오 파일 삭제: {}", tempAudioExtractedFile.getAbsolutePath());
            }
            if (tempAudioCompressedFile != null && tempAudioCompressedFile.exists()) {
                Files.deleteIfExists(tempAudioCompressedPath);
                log.info("임시 압축 오디오 파일 삭제: {}", tempAudioCompressedFile.getAbsolutePath());
            }
        }
    }
}
