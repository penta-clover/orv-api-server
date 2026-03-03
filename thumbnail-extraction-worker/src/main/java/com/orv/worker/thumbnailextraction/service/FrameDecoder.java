package com.orv.worker.thumbnailextraction.service;

import java.awt.image.BufferedImage;

public interface FrameDecoder {

    BufferedImage decode(byte[] codecConfig, byte[] sampleData, int nalLengthSize, int width, int height);

    boolean supports(String codecType);
}
