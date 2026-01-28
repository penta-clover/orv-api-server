package com.orv.api.domain.reservation.repository;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.orv.api.domain.reservation.ReservationStatus;
import com.orv.api.domain.reservation.service.dto.InterviewReservation;

public interface InterviewReservationRepository {
    Optional<UUID> reserveInterview(UUID memberId, UUID storyboardId, LocalDateTime scheduledAt);
    Optional<UUID> reserveInterview(UUID memberId, UUID storyboardId, LocalDateTime scheduledAt, ReservationStatus status);
    Optional<List<InterviewReservation>> getReservedInterviews(UUID member, OffsetDateTime from);
    boolean changeInterviewReservationStatus(UUID reservationId, ReservationStatus status);

    Optional<InterviewReservation> findInterviewReservationById(UUID reservationId);
}
