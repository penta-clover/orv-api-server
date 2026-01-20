package com.orv.api.domain.storyboard.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StoryboardResponse {
    private UUID id;
    private String title;
    private UUID startSceneId;
}
