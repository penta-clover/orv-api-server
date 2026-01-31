package com.orv.storyboard.orchestrator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TopicResponse {
    private UUID id;
    private String name;
    private String description;
    private String thumbnailUrl;
    private List<HashtagResponse> hashtags;
}
