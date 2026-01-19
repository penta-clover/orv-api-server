package com.orv.api.domain.reservation.service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RecapAnswerSummaryInfo {
    private UUID sceneId;
    private String question;
    private String answerSummary;
}
