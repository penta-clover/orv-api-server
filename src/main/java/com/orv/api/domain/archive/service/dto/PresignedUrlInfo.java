package com.orv.api.domain.archive.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class PresignedUrlInfo {
    private String videoId;
    private String uploadUrl;
    private Instant expiresAt;
}
