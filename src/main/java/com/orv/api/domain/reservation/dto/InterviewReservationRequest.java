package com.orv.api.domain.reservation.dto;

import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class InterviewReservationRequest {
    private String storyboardId;
    private ZonedDateTime reservedAt;
}
