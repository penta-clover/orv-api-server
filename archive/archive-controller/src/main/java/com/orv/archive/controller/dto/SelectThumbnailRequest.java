package com.orv.archive.controller.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SelectThumbnailRequest {
    @NotNull(message = "candidateId는 필수입니다")
    private Long candidateId;
}
