package com.orv.api.domain.storyboard.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class Storyboard {
    private UUID id;
    private String title;
    private UUID startSceneId;
}
