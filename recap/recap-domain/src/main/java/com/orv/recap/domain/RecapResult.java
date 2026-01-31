package com.orv.recap.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
public class RecapResult {

    private final UUID id;
    private final OffsetDateTime createdAt;

    @Builder
    public RecapResult(UUID id, OffsetDateTime createdAt) {
        this.id = id;
        this.createdAt = createdAt;
    }
}
