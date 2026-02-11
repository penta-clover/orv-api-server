package com.orv.archive.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Video {
    private UUID id;
    private UUID storyboardId;
    private UUID memberId;
    private String videoUrl;
    private String videoFileKey;
    private LocalDateTime createdAt;
    private String thumbnailUrl;
    private String thumbnailFileKey;
    private Integer runningTime;
    private String title;
    private String status;
}
