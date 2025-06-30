package com.orv.api.domain.media;

import com.orv.api.domain.media.dto.InterviewAudioRecording;
import com.orv.api.domain.media.repository.InterviewAudioRecordingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@Slf4j
public class AudioService {

    private final AudioExtractService audioExtractService;
    private final AudioCompressionService audioCompressionService;
    private final InterviewAudioRecordingRepository audioRecordingRepository;

    public AudioService(AudioExtractService audioExtractService,
                        AudioCompressionService audioCompressionService,
                        InterviewAudioRecordingRepository audioRecordingRepository) {
        this.audioExtractService = audioExtractService;
        this.audioCompressionService = audioCompressionService;
        this.audioRecordingRepository = audioRecordingRepository;
    }

    public InterviewAudioRecording processAndSaveAudio(
            File inputVideoFile, UUID storyboardId, UUID memberId, String s3BaseUrl) throws IOException {

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

            // TODO: 4. 압축된 오디오 파일을 S3에 업로드 (현재 S3 저장 로직 부재, 추후 추가 필요)
            // 임시로 S3 경로를 생성
            String s3Key = "audio-recordings/" + UUID.randomUUID().toString() + ".opus";
            String videoUrl = s3BaseUrl + "/" + s3Key; // 실제 S3 URL이 될 경로

            // TODO: 5. running_time 계산 (FFmpeg 등으로 정확한 시간 획득 필요)
            // 현재는 임시값 (예: 60초)
            int runningTime = 60; // 실제 오디오 길이를 측정하는 로직 필요

            // 6. 메타데이터 저장
            InterviewAudioRecording audioRecording = InterviewAudioRecording.builder()
                    .id(UUID.randomUUID()) // 새로운 UUID 생성
                    .storyboardId(storyboardId)
                    .memberId(memberId)
                    .videoUrl(videoUrl)
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
