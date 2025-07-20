package com.orv.api.domain.reservation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RecapServerRequest {
    @JsonProperty("audio_s3_url")
    private String audioS3Url;

    @JsonProperty("interview_scenario")
    private InterviewScenario interviewScenario;
}
