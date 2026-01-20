package com.orv.api.domain.reservation.orchestrator;

import com.orv.api.domain.reservation.controller.dto.*;
import com.orv.api.domain.reservation.service.RecapService;
import com.orv.api.domain.reservation.service.ReservationService;
import com.orv.api.domain.reservation.service.dto.InterviewReservation;
import com.orv.api.domain.reservation.service.dto.RecapAudioInfo;
import com.orv.api.domain.reservation.service.dto.RecapResultInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ReservationOrchestrator {
    private final ReservationService reservationService;
    private final RecapService recapService;

    public Optional<InterviewReservationResponse> reserveInterview(UUID memberId, UUID storyboardId, OffsetDateTime scheduledAt) throws Exception {
        Optional<UUID> reservationId = reservationService.reserveInterview(memberId, storyboardId, scheduledAt);
        return reservationId.map(id -> new InterviewReservationResponse(
                id,
                memberId,
                storyboardId,
                scheduledAt.toLocalDateTime(),
                LocalDateTime.now()
        ));
    }

    public Optional<InterviewReservationResponse> reserveInstantInterview(UUID memberId, UUID storyboardId) throws Exception {
        Optional<UUID> reservationId = reservationService.reserveInstantInterview(memberId, storyboardId);
        return reservationId.map(id -> new InterviewReservationResponse(
                id,
                memberId,
                storyboardId,
                LocalDateTime.now(),
                LocalDateTime.now()
        ));
    }

    public Optional<InterviewReservationResponse> getInterviewReservationById(UUID reservationId) {
        return reservationService.getInterviewReservationById(reservationId)
                .map(this::toInterviewReservationResponse);
    }

    public Optional<List<InterviewReservationResponse>> getForwardInterviews(UUID memberId, OffsetDateTime from) {
        return reservationService.getForwardInterviews(memberId, from)
                .map(list -> list.stream()
                        .map(this::toInterviewReservationResponse)
                        .collect(Collectors.toList()));
    }

    public boolean markInterviewAsDone(UUID interviewId) {
        return reservationService.markInterviewAsDone(interviewId);
    }

    public Optional<RecapReservationResponse> reserveRecap(UUID memberId, UUID videoId, ZonedDateTime scheduledAt) throws IOException {
        Optional<UUID> recapId = recapService.reserveRecap(memberId, videoId, scheduledAt);
        return recapId.map(id -> new RecapReservationResponse(
                id,
                memberId,
                videoId,
                scheduledAt.toLocalDateTime(),
                LocalDateTime.now()
        ));
    }

    public Optional<RecapResultResponse> getRecapResult(UUID recapReservationId) {
        return recapService.getRecapResult(recapReservationId)
                .map(this::toRecapResultResponse);
    }

    public Optional<RecapAudioResponse> getRecapAudio(UUID recapReservationId) {
        return recapService.getRecapAudio(recapReservationId)
                .map(this::toRecapAudioResponse);
    }

    private InterviewReservationResponse toInterviewReservationResponse(InterviewReservation reservation) {
        return new InterviewReservationResponse(
                reservation.getId(),
                reservation.getMemberId(),
                reservation.getStoryboardId(),
                reservation.getScheduledAt(),
                reservation.getCreatedAt()
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

    private RecapAudioResponse toRecapAudioResponse(RecapAudioInfo info) {
        return new RecapAudioResponse(
                info.getAudioId(),
                info.getAudioUrl(),
                info.getRunningTime(),
                info.getCreatedAt()
        );
    }
}
