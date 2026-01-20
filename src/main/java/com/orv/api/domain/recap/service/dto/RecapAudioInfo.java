package com.orv.api.domain.recap.service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RecapAudioInfo {
    private UUID audioId;
    private String audioUrl;
    private Integer runningTime;
    private OffsetDateTime createdAt;
}
