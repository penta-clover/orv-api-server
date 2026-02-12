package com.orv.archive.orchestrator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ThumbnailCandidateResponse {
    private Long id;
    private Long timestampMs;
    private String imageUrl;
    private LocalDateTime createdAt;
}
