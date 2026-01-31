package com.orv.auth.domain;

import lombok.Data;

@Data
public class ValidationResult {
    private String nickname;
    private Boolean isExists;
    private Boolean isValid;
}
