package com.orv.api.domain.reservation.dto;

import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class RecapReservationRequest {
    private String videoId;
    private ZonedDateTime scheduledAt;
}
