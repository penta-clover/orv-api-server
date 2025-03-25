package com.orv.api.domain.reservation;

import com.orv.api.domain.archive.dto.ImageMetadata;
import com.orv.api.domain.archive.dto.Video;
import com.orv.api.domain.archive.dto.VideoMetadata;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository {
    Optional<UUID> reserveInterview(UUID memberId, UUID storyboardId, LocalDateTime scheduledAt);
}
