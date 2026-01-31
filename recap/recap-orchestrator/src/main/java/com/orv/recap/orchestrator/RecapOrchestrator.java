package com.orv.recap.orchestrator;

import com.orv.recap.external.archive.RecapArchiveApi;
import com.orv.recap.external.media.RecapAudioApi;
import com.orv.recap.external.storyboard.RecapStoryboardApi;
import com.orv.recap.orchestrator.dto.*;
import com.orv.recap.external.RecapClient;
import com.orv.recap.service.RecapService;
import com.orv.recap.domain.RecapAudioInfo;
import com.orv.recap.domain.RecapResultInfo;
import com.orv.recap.domain.InterviewScenario;
import com.orv.storyboard.service.InterviewScenarioConverter;
import com.orv.recap.external.dto.RecapServerRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
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

    public Optional<RecapReservationResponse> reserveRecap(UUID memberId, UUID videoId, ZonedDateTime scheduledAt) throws IOException {
        // 1. Create Recap Reservation in DB
        Optional<UUID> recapIdOptional = recapService.reserveRecap(memberId, videoId, scheduledAt);
        if (recapIdOptional.isEmpty()) {
            return Optional.empty();
        }
        UUID recapReservationId = recapIdOptional.get();

        // 2. Get Video Info & Stream
        RecapArchiveApi.VideoInfo video = archiveApi.getVideo(videoId)
                .orElseThrow(() -> new IOException("Video with ID " + videoId + " not found."));

        Optional<InputStream> videoStreamOptional = archiveApi.getVideoStream(videoId);
        if (videoStreamOptional.isEmpty()) {
            throw new IOException("Failed to retrieve video stream for video ID " + videoId);
        }

        // 3. Extract and save audio
        RecapAudioApi.AudioRecordingInfo audioRecording;
        try (InputStream videoStream = videoStreamOptional.get()) {
             audioRecording = audioApi.extractAndSaveAudioFromVideo(
                    videoStream,
                    video.getStoryboardId(),
                    memberId,
                    video.getTitle() != null ? video.getTitle() + " (Recap Audio)" : "Recap Audio",
                    video.getRunningTime()
            );
        }

        // 4. Link Audio to Reservation
        recapService.linkAudioRecording(recapReservationId, audioRecording.getId());

        // 5. Call Recap Server
        callRecapServer(recapReservationId, video, audioRecording.getAudioUrl());

        return Optional.of(new RecapReservationResponse(
                recapReservationId,
                memberId,
                videoId,
                scheduledAt.toLocalDateTime(),
                LocalDateTime.now()
        ));
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

    public Optional<RecapResultResponse> getRecapResult(UUID recapReservationId) {
        return recapService.getRecapResult(recapReservationId)
                .map(this::toRecapResultResponse);
    }

    public Optional<RecapAudioResponse> getRecapAudio(UUID recapReservationId) {
        return recapService.getRecapAudio(recapReservationId)
                .map(this::toRecapAudioResponse);
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

    private RecapAudioResponse toRecapAudioResponse(RecapAudioInfo info) {
        return new RecapAudioResponse(
                info.getAudioId(),
                info.getAudioUrl(),
                info.getRunningTime(),
                info.getCreatedAt()
        );
    }
}
