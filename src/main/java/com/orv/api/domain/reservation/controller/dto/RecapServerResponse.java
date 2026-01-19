package com.orv.api.domain.reservation.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.orv.api.domain.reservation.service.dto.RecapContent;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RecapServerResponse {
    @JsonProperty("recap_content")
    private List<RecapContent> recapContent;
}
