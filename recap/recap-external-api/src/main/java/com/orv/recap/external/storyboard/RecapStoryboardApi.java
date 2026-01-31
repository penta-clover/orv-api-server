package com.orv.recap.external.storyboard;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Cross-domain API for Recap to access Storyboard domain
 */
public interface RecapStoryboardApi {
    /**
     * Get storyboard by ID
     */
    Optional<StoryboardInfo> getStoryboard(UUID storyboardId);

    /**
     * Get all scenes for a storyboard
     */
    Optional<List<SceneInfo>> getScenes(UUID storyboardId);

    /**
     * Storyboard info DTO
     */
    class StoryboardInfo {
        private final UUID id;
        private final String title;

        public StoryboardInfo(UUID id, String title) {
            this.id = id;
            this.title = title;
        }

        public UUID getId() { return id; }
        public String getTitle() { return title; }
    }

    /**
     * Scene info DTO
     */
    class SceneInfo {
        private final UUID id;
        private final UUID storyboardId;
        private final String question;

        public SceneInfo(UUID id, UUID storyboardId, String question) {
            this.id = id;
            this.storyboardId = storyboardId;
            this.question = question;
        }

        public UUID getId() { return id; }
        public UUID getStoryboardId() { return storyboardId; }
        public String getQuestion() { return question; }
    }
}
