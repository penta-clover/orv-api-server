package com.orv.api.domain.reservation.dto;

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
    @JsonProperty("recap_result_id")
    private UUID recapResultId;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("answer_summaries")
    private List<RecapAnswerSummaryResponse> answerSummaries;
}
