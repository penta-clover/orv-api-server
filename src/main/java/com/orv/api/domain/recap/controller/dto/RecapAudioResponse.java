package com.orv.api.domain.recap.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RecapAudioResponse {
    @JsonProperty("audioId")
    private UUID audioId;

    @JsonProperty("audioUrl")
    private String audioUrl;

    @JsonProperty("runningTime")
    private Integer runningTime;

    @JsonProperty("createdAt")
    private OffsetDateTime createdAt;
}
