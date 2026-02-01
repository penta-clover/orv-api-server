package com.orv.archive.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.InputStream;

@RequiredArgsConstructor
@Getter
public class InputStreamWithMetadata {
    private final InputStream thumbnailImage;
    private final ImageMetadata metadata;
}
