package com.orv.api.domain.recap.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RecapContent {
    @JsonProperty("scene_id")
    private UUID sceneId;

    @JsonProperty("answer_summary")
    private String answerSummary;
}
