package com.orv.api.domain.recap.service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RecapResultInfo {
    private UUID recapResultId;
    private OffsetDateTime createdAt;
    private List<RecapAnswerSummaryInfo> answerSummaries;
}
