package com.orv.api.domain.storyboard.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Array;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoryboardPreview {
    private UUID storyboardId;
    private Array examples;
}
