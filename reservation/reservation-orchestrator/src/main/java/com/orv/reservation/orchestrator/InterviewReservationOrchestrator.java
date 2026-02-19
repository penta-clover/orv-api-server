package com.orv.reservation.orchestrator;

import com.orv.common.dto.PaginatedResponse;
import com.orv.reservation.common.ReservationErrorCode;
import com.orv.reservation.common.ReservationException;
import com.orv.reservation.orchestrator.dto.*;
import com.orv.reservation.service.ReservationNotificationService;
import com.orv.reservation.service.InterviewReservationService;
import com.orv.reservation.domain.InterviewReservation;
import com.orv.storyboard.common.StoryboardErrorCode;
import com.orv.storyboard.common.StoryboardException;
import com.orv.storyboard.domain.Storyboard;
import com.orv.storyboard.domain.StoryboardStatus;
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
        // 1. Validate storyboard status
        validateStoryboardActive(storyboardId);

        // 2. Create Reservation
        Optional<UUID> reservationId = reservationService.reserveInterview(memberId, storyboardId, scheduledAt);
        if (reservationId.isEmpty()) {
            throw new Exception("Failed to reserve interview");
        }

        // 3. Send Notification
        reservationNotificationService.sendInterviewConfirmation(memberId, storyboardId, reservationId.get(), scheduledAt);

        return reservationId.map(id -> new InterviewReservationResponse(
                id,
                memberId,
                storyboardId,
                scheduledAt.toLocalDateTime(),
                LocalDateTime.now(),
                false
        ));
    }

    public Optional<InterviewReservationResponse> reserveInstantInterview(UUID memberId, UUID storyboardId) throws Exception {
        // 1. Validate storyboard status
        validateStoryboardActive(storyboardId);

        // 2. Create Instant Reservation
        Optional<UUID> reservationId = reservationService.reserveInstantInterview(memberId, storyboardId);
        if (reservationId.isEmpty()) {
            throw new Exception("Failed to reserve instant interview");
        }

        // 3. Send Notification (Immediate)
        reservationNotificationService.sendInstantInterviewPreview(memberId, storyboardId, reservationId.get());

        return reservationId.map(id -> new InterviewReservationResponse(
                id,
                memberId,
                storyboardId,
                LocalDateTime.now(),
                LocalDateTime.now(),
                false
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

    public PaginatedResponse<InterviewReservationResponse> getReservations(UUID memberId, OffsetDateTime from, OffsetDateTime to, String sort, int page, int size, Boolean isUsed) {
        List<InterviewReservation> reservations = reservationService.getReservations(memberId, from, to, sort, size + 1, page * size, isUsed);
        boolean hasNext = reservations.size() > size;
        List<InterviewReservation> trimmed = hasNext ? reservations.subList(0, size) : reservations;
        List<InterviewReservationResponse> content = trimmed.stream()
                .map(this::toInterviewReservationResponse)
                .collect(Collectors.toList());
        return new PaginatedResponse<>(content, page, size, hasNext);
    }

    @Transactional
    public void startReservation(UUID reservationId, UUID memberId) {
        InterviewReservation reservation = reservationService.getInterviewReservationById(reservationId)
                .orElseThrow(() -> new ReservationException(ReservationErrorCode.RESERVATION_NOT_FOUND));
        validateStoryboardActive(reservation.getStoryboardId());
        reservationService.markAsUsed(reservationId);
        participateStoryboard(reservation.getStoryboardId(), memberId);
    }

    private void participateStoryboard(UUID storyboardId, UUID memberId) {
        try {
            storyboardService.participateStoryboard(storyboardId, memberId);
        } catch (StoryboardException e) {
            ReservationErrorCode errorCode = switch (e.getErrorCode()) {
                case PARTICIPATION_LIMIT_EXCEEDED -> ReservationErrorCode.PARTICIPATION_LIMIT_EXCEEDED;
                case STORYBOARD_NOT_FOUND, STORYBOARD_NOT_ACTIVE -> ReservationErrorCode.STORYBOARD_NOT_AVAILABLE;
            };

            throw new ReservationException(errorCode);
        }
    }

    private void validateStoryboardActive(UUID storyboardId) {
        Storyboard storyboard = storyboardService.getStoryboard(storyboardId)
                .orElseThrow(() -> new ReservationException(ReservationErrorCode.STORYBOARD_NOT_AVAILABLE));

        if (storyboard.getStatus() != StoryboardStatus.ACTIVE) {
            throw new ReservationException(ReservationErrorCode.STORYBOARD_NOT_AVAILABLE);
        }
    }

    private InterviewReservationResponse toInterviewReservationResponse(InterviewReservation reservation) {
        return new InterviewReservationResponse(
                reservation.getId(),
                reservation.getMemberId(),
                reservation.getStoryboardId(),
                reservation.getScheduledAt(),
                reservation.getCreatedAt(),
                reservation.isUsed()
        );
    }
}
