package com.orv.api.domain.reservation.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InterviewReservationResponse {
    private UUID id;
    private UUID memberId;
    private UUID storyboardId;
    private LocalDateTime scheduledAt;
    private LocalDateTime createdAt;
}
