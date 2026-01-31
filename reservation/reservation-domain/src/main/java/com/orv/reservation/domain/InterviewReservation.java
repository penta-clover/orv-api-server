package com.orv.reservation.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterviewReservation {
    private UUID id;
    private UUID memberId;
    private UUID storyboardId;
    private LocalDateTime scheduledAt;
    private LocalDateTime createdAt;
}
