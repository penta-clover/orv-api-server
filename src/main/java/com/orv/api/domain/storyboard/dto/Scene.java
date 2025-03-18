package com.orv.api.domain.storyboard.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class Scene {
    private UUID id;
    private String name;
    private String sceneType;
    private String content;
    private UUID storyboardId;
}
