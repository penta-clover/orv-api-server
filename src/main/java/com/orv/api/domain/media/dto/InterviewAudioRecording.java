package com.orv.api.domain.media.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewAudioRecording {
    private UUID id;
    private UUID storyboardId;
    private UUID memberId;
    private String videoUrl;
    private OffsetDateTime createdAt;
    private Integer runningTime;
}
