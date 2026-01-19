package com.orv.api.domain.reservation.service.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
public class RecapAnswerSummary {

    private final UUID id;
    private final UUID recapResultId;
    private final UUID sceneId;
    private final String summary;
    private final int sceneOrder;
    private final OffsetDateTime createdAt;

    @Builder
    public RecapAnswerSummary(UUID id, UUID recapResultId, UUID sceneId, String summary, int sceneOrder, OffsetDateTime createdAt) {
        this.id = id;
        this.recapResultId = recapResultId;
        this.sceneId = sceneId;
        this.summary = summary;
        this.sceneOrder = sceneOrder;
        this.createdAt = createdAt;
    }
}
