package com.orv.archive.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ImageMetadata {
    private String contentType;
    private long contentLength;
}
