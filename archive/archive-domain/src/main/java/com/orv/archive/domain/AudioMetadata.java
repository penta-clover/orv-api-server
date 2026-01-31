package com.orv.archive.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class AudioMetadata {
    private UUID storyboardId;
    private UUID ownerId;
    private String title;
    private String contentType;
    private Integer runningTime;
    private long contentLength;
}
