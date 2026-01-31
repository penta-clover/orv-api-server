package com.orv.auth.orchestrator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ValidationResultResponse {
    private String nickname;
    private Boolean isExists;
    private Boolean isValid;
}
