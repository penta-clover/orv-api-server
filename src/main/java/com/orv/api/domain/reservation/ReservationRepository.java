package com.orv.api.domain.reservation;

import com.orv.api.domain.reservation.dto.InterviewReservation;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository {
    Optional<UUID> reserveInterview(UUID memberId, UUID storyboardId, LocalDateTime scheduledAt);
    Optional<List<InterviewReservation>> getReservedInterviews(UUID member, OffsetDateTime from);
    boolean changeInterviewReservationStatus(UUID reservationId, String status);

    Optional<InterviewReservation> findInterviewReservationById(UUID reservationId);
}
