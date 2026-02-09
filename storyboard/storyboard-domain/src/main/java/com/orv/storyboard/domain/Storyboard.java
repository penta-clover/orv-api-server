package com.orv.storyboard.domain;

import lombok.Data;

import java.util.UUID;

@Data
public class Storyboard {
    private UUID id;
    private String title;
    private UUID startSceneId;
    private StoryboardStatus status;
}
