package com.orv.media.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AudioExtractionJob {
    private Long id;
    private UUID videoId;
    private UUID recapReservationId;
    private UUID memberId;
    private UUID storyboardId;
    private UUID resultAudioRecordingId;
    private AudioExtractionJobStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
}
