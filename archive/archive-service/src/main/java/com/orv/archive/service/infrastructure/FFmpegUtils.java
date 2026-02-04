package com.orv.archive.service.infrastructure;

import org.bytedeco.javacv.FFmpegFrameGrabber;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class FFmpegUtils {

    private FFmpegUtils() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    public static void safeClose(FFmpegFrameGrabber grabber) {
        if (grabber != null) {
            try {
                grabber.stop();
            } catch (Exception e) {
                log.warn("Failed to stop FFmpegFrameGrabber", e);
            }
            try {
                grabber.close();
            } catch (Exception e) {
                log.warn("Failed to close FFmpegFrameGrabber", e);
            }
        }
    }
}
