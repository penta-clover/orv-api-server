package com.orv.recap.orchestrator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecapReservationResponse {
    private UUID id;
    private UUID memberId;
    private UUID videoId;
    private LocalDateTime scheduledAt;
    private LocalDateTime createdAt;
}
