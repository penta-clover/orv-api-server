package com.orv.recap.orchestrator;

import com.orv.recap.common.RecapErrorCode;
import com.orv.recap.common.RecapException;
import com.orv.recap.external.archive.RecapArchiveApi;
import com.orv.recap.external.media.RecapAudioApi;
import com.orv.recap.external.storyboard.RecapStoryboardApi;
import com.orv.recap.orchestrator.dto.*;
import com.orv.recap.external.RecapClient;
import com.orv.recap.service.RecapService;
import com.orv.recap.domain.RecapAudioInfo;
import com.orv.recap.domain.RecapResultInfo;
import com.orv.recap.domain.InterviewScenario;
import com.orv.media.repository.AudioExtractionJobRepository;
import com.orv.storyboard.service.InterviewScenarioConverter;
import com.orv.recap.external.dto.RecapServerRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecapOrchestrator {
    private final RecapService recapService;
    private final RecapArchiveApi archiveApi;
    private final RecapAudioApi audioApi;
    private final RecapStoryboardApi storyboardApi;
    private final InterviewScenarioConverter interviewScenarioFactory;
    private final RecapClient recapClient;
    private final AudioExtractionJobRepository audioExtractionJobRepository;

    public RecapReservationResponse reserveRecap(UUID memberId, UUID videoId, ZonedDateTime scheduledAt) {
        // 1. Create Recap Reservation in DB
        UUID recapReservationId = recapService.reserveRecap(memberId, videoId, scheduledAt)
                .orElseThrow(() -> new RecapException(RecapErrorCode.RECAP_RESERVATION_FAILED));

        // 2. Get Video Info & Stream
        RecapArchiveApi.VideoInfo video = archiveApi.getVideo(videoId)
                .orElseThrow(() -> new RecapException(RecapErrorCode.VIDEO_NOT_FOUND));

        Optional<InputStream> videoStreamOptional = archiveApi.getVideoStream(videoId);
        if (videoStreamOptional.isEmpty()) {
            throw new RecapException(RecapErrorCode.VIDEO_NOT_FOUND);
        }

        // 3. Extract and save audio
        RecapAudioApi.AudioRecordingInfo audioRecording;
        File tempVideoFile;
        try {
            tempVideoFile = File.createTempFile("recap_video_", ".mp4");
        } catch (IOException e) {
            throw new RecapException(RecapErrorCode.AUDIO_EXTRACTION_FAILED, e);
        }

        try (InputStream videoStream = videoStreamOptional.get()) {
            Files.copy(videoStream, tempVideoFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            audioRecording = audioApi.extractAndSaveAudioFromVideo(
                    tempVideoFile,
                    video.getStoryboardId(),
                    memberId
            );
        } catch (RecapException e) {
            throw e;
        } catch (IOException e) {
            throw new RecapException(RecapErrorCode.AUDIO_EXTRACTION_FAILED, e);
        } finally {
            try { Files.deleteIfExists(tempVideoFile.toPath()); } catch (IOException ignored) {}
        }

        // 4. Link Audio to Reservation
        recapService.linkAudioRecording(recapReservationId, audioRecording.getId());

        // 5. Call Recap Server
        String audioUrl = audioApi.resolveAudioUrl(audioRecording.getAudioFileKey());
        callRecapServer(recapReservationId, video, audioUrl);

        return new RecapReservationResponse(
                recapReservationId,
                memberId,
                videoId,
                scheduledAt.toLocalDateTime(),
                LocalDateTime.now()
        );
    }

    @Transactional
    public RecapReservationResponse reserveRecapAsync(UUID memberId, UUID videoId, ZonedDateTime scheduledAt) {
        // 1. Create Recap Reservation in DB
        UUID recapReservationId = recapService.reserveRecap(memberId, videoId, scheduledAt)
                .orElseThrow(() -> new RecapException(RecapErrorCode.RECAP_RESERVATION_FAILED));

        // 2. Get Video Info (metadata only, no stream download)
        RecapArchiveApi.VideoInfo video = archiveApi.getVideo(videoId)
                .orElseThrow(() -> new RecapException(RecapErrorCode.VIDEO_NOT_FOUND));

        // 3. Enqueue audio extraction job (async processing by worker)
        audioExtractionJobRepository.create(
                videoId,
                recapReservationId,
                memberId,
                video.getStoryboardId()
        );

        return new RecapReservationResponse(
                recapReservationId,
                memberId,
                videoId,
                scheduledAt.toLocalDateTime(),
                LocalDateTime.now()
        );
    }

    private void callRecapServer(UUID recapReservationId, RecapArchiveApi.VideoInfo video, String audioS3Url) {
        try {
            // 1. Get storyboard and scene info
            RecapStoryboardApi.StoryboardInfo storyboard = storyboardApi.getStoryboard(video.getStoryboardId())
                    .orElseThrow(() -> new RuntimeException("Storyboard not found for ID: " + video.getStoryboardId()));

            List<RecapStoryboardApi.SceneInfo> allScenes = storyboardApi.getScenes(video.getStoryboardId())
                    .orElseThrow(() -> new RuntimeException("Scenes not found for Storyboard ID: " + video.getStoryboardId()));

            // 2. Create InterviewScenario using the factory
            InterviewScenario interviewScenario = interviewScenarioFactory.createFromApi(storyboard, allScenes);

            // 3. Create request body
            RecapServerRequest requestBody = new RecapServerRequest(audioS3Url, interviewScenario);

            // 4. Call API via RecapClient
            /*
            recapClient.requestRecap(requestBody).ifPresent(response -> {
                log.info("Successfully received recap from server for video ID: {}. Storing results...", video.getId());
                recapService.saveRecapResult(recapReservationId, response.getRecapContent())
                        .ifPresent(recapResultId -> log.info("Recap results stored successfully with result ID: {}", recapResultId));
            });
            */
            log.info("Prepared to call Recap Server for reservation ID: {}", recapReservationId);

        } catch (Exception e) {
            log.error("Failed to prepare/call recap server for reservation ID: {}", recapReservationId, e);
        }
    }

    public RecapResultResponse getRecapResult(UUID recapReservationId) {
        return recapService.getRecapResult(recapReservationId)
                .map(this::toRecapResultResponse)
                .orElseThrow(() -> new RecapException(RecapErrorCode.RECAP_RESULT_NOT_FOUND));
    }

    public RecapAudioResponse getRecapAudio(UUID recapReservationId) {
        RecapAudioInfo info = recapService.getRecapAudio(recapReservationId)
                .orElseThrow(() -> new RecapException(RecapErrorCode.RECAP_AUDIO_NOT_FOUND));

        String audioUrl = audioApi.resolveAudioUrl(info.getAudioFileKey());
        return new RecapAudioResponse(
                info.getAudioId(),
                audioUrl,
                info.getRunningTime(),
                info.getCreatedAt()
        );
    }

    private RecapResultResponse toRecapResultResponse(RecapResultInfo info) {
        List<RecapAnswerSummaryResponse> summaries = info.getAnswerSummaries().stream()
                .map(summary -> new RecapAnswerSummaryResponse(
                        summary.getSceneId(),
                        summary.getQuestion(),
                        summary.getAnswerSummary()
                ))
                .collect(Collectors.toList());

        return new RecapResultResponse(
                info.getRecapResultId(),
                info.getCreatedAt(),
                summaries
        );
    }
}
