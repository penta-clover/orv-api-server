package com.orv.api.domain.reservation.service;

import com.orv.api.domain.archive.repository.AudioRepository;
import com.orv.api.domain.archive.repository.VideoRepository;
import com.orv.api.domain.archive.service.dto.AudioMetadata;
import com.orv.api.domain.archive.service.dto.Video;
import com.orv.api.domain.media.repository.InterviewAudioRecordingRepository;
import com.orv.api.domain.media.service.AudioCompressionService;
import com.orv.api.domain.media.service.AudioExtractService;
import com.orv.api.domain.media.service.dto.InterviewAudioRecording;
import com.orv.api.domain.reservation.controller.dto.RecapServerRequest;
import com.orv.api.domain.reservation.repository.RecapRepository;
import com.orv.api.domain.reservation.repository.RecapResultRepository;
import com.orv.api.domain.reservation.service.dto.*;
import com.orv.api.domain.storyboard.repository.StoryboardRepository;
import com.orv.api.domain.storyboard.service.InterviewScenarioConverter;
import com.orv.api.domain.storyboard.service.dto.Scene;
import com.orv.api.domain.storyboard.service.dto.Storyboard;
import com.orv.api.infra.recap.RecapClient;
import lombok.RequiredArgsConstructor;
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
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecapServiceImpl implements RecapService {

    private final VideoRepository videoRepository;
    private final AudioExtractService audioExtractService;
    private final AudioCompressionService audioCompressionService;
    private final AudioRepository audioRepository;
    private final InterviewAudioRecordingRepository interviewAudioRecordingRepository;
    private final RecapRepository recapRepository;
    private final RecapResultRepository recapResultRepository;
    private final StoryboardRepository storyboardRepository;
    private final InterviewScenarioConverter interviewScenarioFactory;
    private final RecapClient recapClient;

    @Override
    public Optional<UUID> reserveRecap(UUID memberId, UUID videoId, ZonedDateTime scheduledAt) throws IOException {
        // 1. DB에 리캡 예약 정보 저장
        Optional<UUID> recapReservationIdOptional = recapRepository.reserveRecap(memberId, videoId, scheduledAt.toLocalDateTime());
        if (recapReservationIdOptional.isEmpty()) {
            log.error("Failed to reserve recap for video ID: {}", videoId);
            return Optional.empty();
        }
        UUID recapReservationId = recapReservationIdOptional.get();
        log.info("Recap reservation saved to DB with ID: {}", recapReservationId);

        // 2. 오디오 처리 및 저장
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IOException("Video with ID " + videoId + " not found."));
       String audioS3Url = processAudio(video, memberId, recapReservationId);

        // 3. Recap 서버 API 호출
       callRecapServer(recapReservationId, video, audioS3Url);

        return recapReservationIdOptional;
    }

    private String processAudio(Video video, UUID memberId, UUID recapReservationId) throws IOException {
        Optional<InputStream> videoStreamOptional = videoRepository.getVideoStream(video.getId());
        if (videoStreamOptional.isEmpty()) {
            log.error("Failed to get video stream for video ID {}.", video.getId());
            throw new IOException("Failed to retrieve video stream.");
        }

        Path tempVideoPath = null;
        Path tempAudioExtractedPath = null;
        Path tempAudioCompressedPath = null;
        File tempVideoFile = null;
        File tempAudioExtractedFile = null;
        File tempAudioCompressedFile = null;

        try (InputStream videoInputStream = videoStreamOptional.get()) {
            // 1. Download video stream to a temporary file
            tempVideoPath = Files.createTempFile("recap_video_", ".mp4");
            tempVideoFile = tempVideoPath.toFile();
            Files.copy(videoInputStream, tempVideoPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            log.info("Video downloaded to temporary file: {}", tempVideoFile.getAbsolutePath());

            // 2. Extract audio (Video -> WAV)
            tempAudioExtractedPath = Files.createTempFile("extracted_audio_", ".wav");
            tempAudioExtractedFile = tempAudioExtractedPath.toFile();
            log.info("Extracting audio from video: {}", tempVideoFile.getAbsolutePath());
            audioExtractService.extractAudio(tempVideoFile, tempAudioExtractedFile, "wav");
            log.info("Audio extracted to: {}", tempAudioExtractedFile.getAbsolutePath());

            // 3. Compress audio (WAV -> Opus)
            tempAudioCompressedPath = Files.createTempFile("compressed_audio_", ".opus");
            tempAudioCompressedFile = tempAudioCompressedPath.toFile();
            log.info("Compressing audio: {}", tempAudioCompressedFile.getAbsolutePath());
            audioCompressionService.compress(tempAudioExtractedFile, tempAudioCompressedFile);
            log.info("Audio compressed to: {}", tempAudioCompressedFile.getAbsolutePath());

            // 4. Upload compressed audio to S3
            URI resourceUrl;
            try (InputStream compressedAudioInputStream = new FileInputStream(tempAudioCompressedFile)) {
                AudioMetadata audioMetadata = new AudioMetadata(
                        video.getStoryboardId(),
                        memberId,
                        video.getTitle() != null ? video.getTitle() + " (Recap Audio)" : "Recap Audio",
                        "audio/opus",
                        video.getRunningTime(), // Reusing video's running time for now
                        tempAudioCompressedFile.length()
                );
                resourceUrl = audioRepository.save(compressedAudioInputStream, audioMetadata)
                        .orElseThrow(() -> new IOException("Failed to upload compressed audio to S3."));
            }
            log.info("Compressed audio uploaded to S3: {}", resourceUrl);

            // 5. Save recap audio metadata to DB
            InterviewAudioRecording recapAudioRecording = InterviewAudioRecording.builder()
                    .id(UUID.randomUUID())
                    .storyboardId(video.getStoryboardId())
                    .memberId(memberId)
                    .audioUrl(resourceUrl.toString()) // Storing S3 audio URL in audioUrl field
                    .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                    .runningTime(video.getRunningTime())
                    .build();
            InterviewAudioRecording savedAudioRecording = interviewAudioRecordingRepository.save(recapAudioRecording);

            if (savedAudioRecording != null) {
                UUID audioRecordingId = savedAudioRecording.getId();
                recapRepository.linkAudioRecording(recapReservationId, audioRecordingId);
                log.info("Recap audio metadata saved and linked to reservation ID: {}", recapReservationId);
            } else {
                log.error("Failed to save recap audio metadata to DB for video ID: {}", video.getId());
                throw new IOException("Failed to save recap audio metadata.");
            }
            return resourceUrl.toString();

        } catch (Exception e) {
            log.error("Error processing audio for video ID {}: {}", video.getId(), e.getMessage(), e);
            throw new IOException("Audio processing failed for video ID: " + video.getId(), e);
        } finally {
            // Clean up temporary files
            if (tempVideoFile != null && tempVideoFile.exists()) {
                Files.deleteIfExists(tempVideoPath);
                log.info("Deleted temporary video file: {}", tempVideoFile.getAbsolutePath());
            }
            if (tempAudioExtractedFile != null && tempAudioExtractedFile.exists()) {
                Files.deleteIfExists(tempAudioExtractedPath);
                log.info("Deleted temporary extracted audio file: {}", tempAudioExtractedFile.getAbsolutePath());
            }
            if (tempAudioCompressedFile != null && tempAudioCompressedFile.exists()) {
                Files.deleteIfExists(tempAudioCompressedPath);
                log.info("Deleted temporary compressed audio file: {}", tempAudioCompressedFile.getAbsolutePath());
            }
        }
    }

    private void callRecapServer(UUID recapReservationId, Video video, String audioS3Url) {
        // 1. Get storyboard and scene info
        Storyboard storyboard = storyboardRepository.findById(video.getStoryboardId())
                .orElseThrow(() -> new RuntimeException("Storyboard not found for ID: " + video.getStoryboardId()));

        List<Scene> allScenes = storyboardRepository.findScenesByStoryboardId(video.getStoryboardId())
                .orElseThrow(() -> new RuntimeException("Scenes not found for Storyboard ID: " + video.getStoryboardId()));

        // 2. Create InterviewScenario using the factory
        InterviewScenario interviewScenario = interviewScenarioFactory.create(storyboard, allScenes);

        // 3. Create request body
        RecapServerRequest requestBody = new RecapServerRequest(audioS3Url, interviewScenario);

        // 4. Call API via RecapClient (보류)
        // recapClient.requestRecap(requestBody).ifPresent(response -> {
        //     log.info("Successfully received recap from server for video ID: {}. Storing results...", video.getId());
        //     recapResultRepository.save(recapReservationId, response.getRecapContent())
        //             .ifPresent(recapResultId -> log.info("Recap results stored successfully with result ID: {}", recapResultId));
        // });
    }

    @Override
    public Optional<RecapResultInfo> getRecapResult(UUID recapReservationId) {
        return recapResultRepository.findByRecapReservationId(recapReservationId)
                .map(response -> new RecapResultInfo(
                        response.getRecapResultId(),
                        response.getCreatedAt(),
                        response.getAnswerSummaries().stream()
                                .map(summary -> new RecapAnswerSummaryInfo(
                                        summary.getSceneId(),
                                        summary.getQuestion(),
                                        summary.getAnswerSummary()
                                ))
                                .collect(Collectors.toList())
                ));
    }

    @Override
    public Optional<RecapAudioInfo> getRecapAudio(UUID recapReservationId) {
        return recapRepository.findAudioByRecapReservationId(recapReservationId)
                .map(audioRecording -> new RecapAudioInfo(
                        audioRecording.getId(),
                        audioRecording.getAudioUrl(),
                        audioRecording.getRunningTime(),
                        audioRecording.getCreatedAt()
                ));
    }
}
