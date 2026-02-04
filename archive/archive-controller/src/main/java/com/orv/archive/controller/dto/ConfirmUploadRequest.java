package com.orv.archive.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConfirmUploadRequest {
    @NotBlank(message = "videoId는 필수입니다")
    private String videoId;
}
