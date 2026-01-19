package com.orv.api.domain.storyboard.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
public class StoryboardPreviewResponse {
    private UUID storyboardId;
    private Integer questionCount;
    private List<String> questions;
}
