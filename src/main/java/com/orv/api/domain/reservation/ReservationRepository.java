package com.orv.api.domain.reservation;

import com.orv.api.domain.archive.dto.ImageMetadata;
import com.orv.api.domain.archive.dto.Video;
import com.orv.api.domain.archive.dto.VideoMetadata;
import com.orv.api.domain.reservation.dto.InterviewReservation;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository {
    Optional<UUID> reserveInterview(UUID memberId, UUID storyboardId, LocalDateTime scheduledAt);
    Optional<List<InterviewReservation>> getReservedInterviews(UUID member);
}
