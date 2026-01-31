package com.orv.recap.orchestrator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RecapResultResponse {
    @JsonProperty("recapResultId")
    private UUID recapResultId;

    @JsonProperty("createdAt")
    private OffsetDateTime createdAt;

    @JsonProperty("answerSummaries")
    private List<RecapAnswerSummaryResponse> answerSummaries;
}
