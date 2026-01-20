package com.orv.api.domain.media.infrastructure;

import java.io.File;
import java.io.IOException;

public interface AudioCompressor {
    void compress(File inputFile, File outputFile) throws IOException;
}
