package com.orv.api.domain.archive.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ImageMetadata {
    private String contentType;
    private long contentLength;
}
