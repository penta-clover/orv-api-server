package com.orv.api.domain.media;

import java.io.File;
import java.io.IOException;

public interface AudioCompressionService {
    void compress(File inputFile, File outputFile) throws IOException;
}
