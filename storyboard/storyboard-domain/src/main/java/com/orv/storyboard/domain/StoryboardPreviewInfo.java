package com.orv.storyboard.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoryboardPreviewInfo {
    private UUID storyboardId;
    private int questionCount;
    private List<String> questions;
}
