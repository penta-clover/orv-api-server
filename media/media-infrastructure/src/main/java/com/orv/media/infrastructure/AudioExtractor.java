package com.orv.media.infrastructure;

import java.io.File;
import java.io.IOException;

public interface AudioExtractor {
    /**
     * Extracts audio from a video file.
     *
     * @return duration of extracted audio in seconds
     */
    int extractAudio(File inputVideoFile, File outputAudioFile, String format) throws IOException;
}
