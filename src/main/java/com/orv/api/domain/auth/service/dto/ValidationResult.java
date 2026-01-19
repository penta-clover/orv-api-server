package com.orv.api.domain.auth.service.dto;

import lombok.Data;

@Data
public class ValidationResult {
    private String nickname;
    private Boolean isExists;
    private Boolean isValid;
}
