package com.orv.api.domain.recap.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RecapAnswerSummaryResponse {
    @JsonProperty("sceneId")
    private UUID sceneId;

    @JsonProperty("question")
    private String question;

    @JsonProperty("answerSummary")
    private String answerSummary;
}
