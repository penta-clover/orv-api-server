package com.orv.reservation.service;

import com.orv.reservation.domain.ReservationStatus;
import com.orv.reservation.repository.InterviewReservationRepository;
import com.orv.reservation.domain.InterviewReservation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InterviewReservationServiceImpl implements InterviewReservationService {
    private final InterviewReservationRepository reservationRepository;

    @Override
    public Optional<InterviewReservation> getInterviewReservationById(UUID reservationId) {
        return reservationRepository.findInterviewReservationById(reservationId);
    }

    @Override
    public Optional<UUID> reserveInterview(UUID memberId, UUID storyboardId, OffsetDateTime reservedAt) throws Exception {
        Optional<UUID> id = reservationRepository.reserveInterview(memberId, storyboardId, reservedAt.toLocalDateTime());

        if (id.isEmpty()) {
            throw new Exception("Failed to reserve interview");
        }
        return id;
    }

    @Override
    public Optional<UUID> reserveInstantInterview(UUID memberId, UUID storyboardId) throws Exception {
        OffsetDateTime scheduledAt = OffsetDateTime.now();
        // Instant interview is scheduled 9 hours later in DB logic from original code, maintaining it.

        Optional<UUID> id = reservationRepository.reserveInterview(memberId, storyboardId, scheduledAt.toLocalDateTime().plusHours(9), ReservationStatus.DONE);
        if (id.isEmpty()) {
             throw new Exception("Failed to reserve instant interview");
        }

        return id;
    }

    @Override
    public Optional<List<InterviewReservation>> getForwardInterviews(UUID memberId, OffsetDateTime from) {
        return reservationRepository.getReservedInterviews(memberId, from);
    }

    @Override
    public boolean markInterviewAsDone(UUID interviewId) {
        return reservationRepository.changeInterviewReservationStatus(interviewId, ReservationStatus.DONE);
    }
}