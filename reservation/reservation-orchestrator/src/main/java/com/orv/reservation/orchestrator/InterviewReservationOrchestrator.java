package com.orv.reservation.orchestrator;

import com.orv.reservation.orchestrator.dto.*;
import com.orv.reservation.service.ReservationNotificationService;
import com.orv.reservation.service.InterviewReservationService;
import com.orv.reservation.domain.InterviewReservation;
import com.orv.storyboard.domain.StoryboardUsageStatus;
import com.orv.storyboard.service.StoryboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class InterviewReservationOrchestrator {
    private final InterviewReservationService reservationService;
    private final ReservationNotificationService reservationNotificationService;
    private final StoryboardService storyboardService;

    public Optional<InterviewReservationResponse> reserveInterview(UUID memberId, UUID storyboardId, OffsetDateTime scheduledAt) throws Exception {
        // 1. Create Reservation
        Optional<UUID> reservationId = reservationService.reserveInterview(memberId, storyboardId, scheduledAt);
        if (reservationId.isEmpty()) {
            throw new Exception("Failed to reserve interview");
        }

        // 2. Send Notification
        reservationNotificationService.sendInterviewConfirmation(memberId, storyboardId, reservationId.get(), scheduledAt);

        return reservationId.map(id -> new InterviewReservationResponse(
                id,
                memberId,
                storyboardId,
                scheduledAt.toLocalDateTime(),
                LocalDateTime.now()
        ));
    }

    public Optional<InterviewReservationResponse> reserveInstantInterview(UUID memberId, UUID storyboardId) throws Exception {
        // 1. Create Instant Reservation
        Optional<UUID> reservationId = reservationService.reserveInstantInterview(memberId, storyboardId);
        if (reservationId.isEmpty()) {
            throw new Exception("Failed to reserve instant interview");
        }

        // 2. Send Notification (Immediate)
        reservationNotificationService.sendInstantInterviewPreview(memberId, storyboardId, reservationId.get());

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

    @Transactional
    public void startReservation(UUID reservationId, UUID memberId) {
        InterviewReservation reservation = reservationService.markAsUsed(reservationId);
        storyboardService.saveUsageHistory(reservation.getStoryboardId(), memberId, StoryboardUsageStatus.STARTED);
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
}
