package com.orv.archive.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VideoDurationCalculationJob {
    private Long id;
    private UUID videoId;
    private JobStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
}
