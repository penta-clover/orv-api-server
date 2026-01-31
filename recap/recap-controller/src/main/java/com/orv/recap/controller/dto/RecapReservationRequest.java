package com.orv.recap.controller.dto;

import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class RecapReservationRequest {
    private String videoId;
    private ZonedDateTime scheduledAt;
}
