package com.orv.reservation.service;

import org.quartz.SchedulerException;
import org.springframework.stereotype.Service;

import com.orv.reservation.domain.InterviewReservation;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public interface InterviewReservationService {
    Optional<InterviewReservation> getInterviewReservationById(UUID reservationId);
    Optional<UUID> reserveInterview(UUID memberId, UUID storyboardId, OffsetDateTime reservedAt) throws Exception;
    Optional<UUID> reserveInstantInterview(UUID memberId, UUID storyboardId) throws Exception;
    Optional<List<InterviewReservation>> getForwardInterviews(UUID memberId, OffsetDateTime from);
    boolean markInterviewAsDone(UUID interviewId);
    int countActiveReservations(UUID memberId, java.time.LocalDateTime startAt, java.time.LocalDateTime endAt);
    InterviewReservation markAsUsed(UUID reservationId);
    List<InterviewReservation> getReservations(UUID memberId, OffsetDateTime from, OffsetDateTime to, String sort, int limit, int offset, Boolean isUsed);
}
