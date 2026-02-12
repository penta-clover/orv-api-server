package com.orv.archive.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ThumbnailCandidate {
    private Long id;
    private Long jobId;
    private UUID videoId;
    private Long timestampMs;
    private String fileKey;
    private LocalDateTime createdAt;
}
