package com.orv.api.domain.archive.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class PresignedUrlResponse {
    private String videoId;
    private String uploadUrl;
    private Instant expiresAt;
}
