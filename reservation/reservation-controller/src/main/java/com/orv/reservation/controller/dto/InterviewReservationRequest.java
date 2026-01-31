package com.orv.reservation.controller.dto;

import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class InterviewReservationRequest {
    private String storyboardId;
    private ZonedDateTime reservedAt;
}
