package com.orv.api.domain.reservation;

import com.orv.api.domain.reservation.dto.InterviewReservation;
import org.quartz.SchedulerException;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public interface ReservationService {
    Optional<UUID> reserveInterview(UUID memberId, UUID storyboardId, ZonedDateTime reservedAt) throws Exception;
    Optional<List<InterviewReservation>> getForwardInterviews(UUID memberId, OffsetDateTime from);
    boolean markInterviewAsDone(UUID interviewId);
    Optional<UUID> reserveRecap(UUID memberId, UUID videoId, ZonedDateTime scheduledAt);
}
