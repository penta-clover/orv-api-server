package com.orv.api.domain.archive.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class VideoMetadata {
    private UUID storyboardId;
    private UUID ownerId;
    private String title;
    private String contentType;
    private long contentLength;
}
