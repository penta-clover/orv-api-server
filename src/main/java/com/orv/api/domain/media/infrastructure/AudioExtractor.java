package com.orv.api.domain.media.infrastructure;

import java.io.File;
import java.io.IOException;

public interface AudioExtractor {
    void extractAudio(File inputVideoFile, File outputAudioFile, String format) throws IOException;
}