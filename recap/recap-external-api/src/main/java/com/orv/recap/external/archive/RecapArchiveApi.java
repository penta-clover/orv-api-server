package com.orv.recap.external.archive;

import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

/**
 * Cross-domain API for Recap to access Archive domain
 * Exposes only the operations needed by Recap domain
 */
public interface RecapArchiveApi {
    /**
     * Get video metadata and details
     */
    Optional<VideoInfo> getVideo(UUID videoId);

    /**
     * Get video stream for processing
     */
    Optional<InputStream> getVideoStream(UUID videoId);

    /**
     * Video info DTO exposed to Recap domain
     */
    class VideoInfo {
        private final UUID id;
        private final UUID storyboardId;
        private final UUID memberId;
        private final String title;
        private final Integer runningTime;

        public VideoInfo(UUID id, UUID storyboardId, UUID memberId, String title, Integer runningTime) {
            this.id = id;
            this.storyboardId = storyboardId;
            this.memberId = memberId;
            this.title = title;
            this.runningTime = runningTime;
        }

        // Getters
        public UUID getId() { return id; }
        public UUID getStoryboardId() { return storyboardId; }
        public UUID getMemberId() { return memberId; }
        public String getTitle() { return title; }
        public Integer getRunningTime() { return runningTime; }
    }
}
