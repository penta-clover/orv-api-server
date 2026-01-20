package com.orv.api.domain.storyboard.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SceneResponse {
    private UUID id;
    private String name;
    private String sceneType;
    private String content;
    private UUID storyboardId;
}
