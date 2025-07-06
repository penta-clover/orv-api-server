package com.orv.api.domain.reservation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RecapAnswerSummaryResponse {
    @JsonProperty("scene_id")
    private UUID sceneId;

    @JsonProperty("question")
    private String question;

    @JsonProperty("answer_summary")
    private String answerSummary;
}
