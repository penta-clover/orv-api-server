package com.orv.reservation.service;

import com.orv.reservation.common.ReservationErrorCode;
import com.orv.reservation.common.ReservationException;
import com.orv.reservation.domain.ReservationStatus;
import com.orv.reservation.repository.InterviewReservationRepository;
import com.orv.reservation.domain.InterviewReservation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewReservationServiceImpl implements InterviewReservationService {
    private final InterviewReservationRepository reservationRepository;

    private static final int BUSINESS_DAY_START_HOUR = 4; // Business day starts at 4:00 AM
    private static final int DAILY_INTERVIEW_LIMIT = 1; // Daily interview limit per user

    @Override
    public Optional<InterviewReservation> getInterviewReservationById(UUID reservationId) {
        return reservationRepository.findInterviewReservationById(reservationId);
    }

    @Override
    public Optional<UUID> reserveInterview(UUID memberId, UUID storyboardId, OffsetDateTime reservedAt) throws Exception {
        // 1. Check daily limit
        validateDailyLimit(memberId, reservedAt);

        // 2. Create reservation
        Optional<UUID> id = reservationRepository.reserveInterview(memberId, storyboardId, reservedAt.toLocalDateTime());

        if (id.isEmpty()) {
            throw new Exception("Failed to reserve interview");
        }
        return id;
    }
    
    private void validateDailyLimit(UUID memberId, OffsetDateTime scheduledAt) throws Exception {
        // Calculate business day boundaries (4:00 AM to next day 4:00 AM)
        LocalDateTime targetTime = scheduledAt.toLocalDateTime();
        LocalDateTime businessDayStart = targetTime.minusHours(BUSINESS_DAY_START_HOUR)
                .toLocalDate()
                .atStartOfDay()
                .plusHours(BUSINESS_DAY_START_HOUR);
        LocalDateTime businessDayEnd = businessDayStart.plusDays(1);

        // Count existing reservations
        int currentCount = reservationRepository.countActiveReservations(memberId, businessDayStart, businessDayEnd);

        log.info("Daily limit check - memberId: {}, date: {}, currentCount: {}, limit: {}",
                memberId, businessDayStart.toLocalDate(), currentCount, DAILY_INTERVIEW_LIMIT);

        if (currentCount >= DAILY_INTERVIEW_LIMIT) {
            throw new Exception("Daily interview limit exceeded. You can only reserve "
                    + DAILY_INTERVIEW_LIMIT + " interview(s) per day.");
        }
    }

    @Override
    public Optional<UUID> reserveInstantInterview(UUID memberId, UUID storyboardId) throws Exception {
        OffsetDateTime scheduledAt = OffsetDateTime.now();

        // Check daily limit for instant interview too
        validateDailyLimit(memberId, scheduledAt);

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

    @Override
    public int countActiveReservations(UUID memberId, LocalDateTime startAt, LocalDateTime endAt) {
        return reservationRepository.countActiveReservations(memberId, startAt, endAt);
    }

    @Override
    @Transactional
    public InterviewReservation markAsUsed(UUID reservationId) {
        InterviewReservation reservation = reservationRepository.findInterviewReservationByIdForUpdate(reservationId)
                .orElseThrow(() -> new ReservationException(ReservationErrorCode.RESERVATION_NOT_FOUND));

        if (reservation.isUsed()) {
            throw new ReservationException(ReservationErrorCode.RESERVATION_ALREADY_USED);
        }

        reservationRepository.markAsUsed(reservationId);

        return reservation;
    }

    @Override
    public List<InterviewReservation> getReservations(UUID memberId, OffsetDateTime from, OffsetDateTime to, String sort, int limit, int offset, Boolean isUsed) {
        return reservationRepository.getReservations(memberId, from, to, sort, limit, offset, isUsed);
    }
}