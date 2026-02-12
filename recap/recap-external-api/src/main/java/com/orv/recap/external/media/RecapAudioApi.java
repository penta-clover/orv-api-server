package com.orv.recap.external.media;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Cross-domain API for Recap to access Media domain audio operations
 */
public interface RecapAudioApi {
    /**
     * Extract audio from video file and save to storage
     */
    AudioRecordingInfo extractAndSaveAudioFromVideo(
        File videoFile,
        UUID storyboardId,
        UUID memberId
    ) throws IOException;

    /**
     * Resolve audio file key to public URL
     */
    String resolveAudioUrl(String audioFileKey);

    /**
     * Audio recording info DTO
     */
    class AudioRecordingInfo {
        private final UUID id;
        private final String audioFileKey;

        public AudioRecordingInfo(UUID id, String audioFileKey) {
            this.id = id;
            this.audioFileKey = audioFileKey;
        }

        public UUID getId() { return id; }
        public String getAudioFileKey() { return audioFileKey; }
    }
}
