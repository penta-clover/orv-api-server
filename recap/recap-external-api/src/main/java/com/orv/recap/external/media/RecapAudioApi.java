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
     * Audio recording info DTO
     */
    class AudioRecordingInfo {
        private final UUID id;
        private final String audioUrl;

        public AudioRecordingInfo(UUID id, String audioUrl) {
            this.id = id;
            this.audioUrl = audioUrl;
        }

        public UUID getId() { return id; }
        public String getAudioUrl() { return audioUrl; }
    }
}
