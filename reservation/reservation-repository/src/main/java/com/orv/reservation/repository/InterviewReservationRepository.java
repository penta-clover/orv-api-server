package com.orv.reservation.repository;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.orv.reservation.domain.ReservationStatus;
import com.orv.reservation.domain.InterviewReservation;

public interface InterviewReservationRepository {
    Optional<UUID> reserveInterview(UUID memberId, UUID storyboardId, LocalDateTime scheduledAt);
    Optional<UUID> reserveInterview(UUID memberId, UUID storyboardId, LocalDateTime scheduledAt, ReservationStatus status);
    Optional<List<InterviewReservation>> getReservedInterviews(UUID member, OffsetDateTime from);
    boolean changeInterviewReservationStatus(UUID reservationId, ReservationStatus status);
    Optional<InterviewReservation> findInterviewReservationById(UUID reservationId);
    int countActiveReservations(UUID memberId, LocalDateTime startAt, LocalDateTime endAt);
}
