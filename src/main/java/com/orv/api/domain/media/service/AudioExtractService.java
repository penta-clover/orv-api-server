package com.orv.api.domain.media.service;

import java.io.File;
import java.io.IOException;

public interface AudioExtractService {
    void extractAudio(File inputVideoFile, File inputAudioFile, String format) throws IOException;
}
