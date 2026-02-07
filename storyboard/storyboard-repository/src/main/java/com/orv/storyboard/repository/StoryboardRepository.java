package com.orv.storyboard.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.orv.storyboard.domain.Scene;
import com.orv.storyboard.domain.Storyboard;
import com.orv.storyboard.domain.StoryboardUsageStatus;
import com.orv.storyboard.domain.Topic;

public interface StoryboardRepository {
    Optional<Storyboard> findById(UUID id);

    Optional<Scene> findSceneById(UUID id);

    Optional<List<Scene>> findScenesByStoryboardId(UUID id);

    Storyboard save(Storyboard storyboard);

    Scene saveScene(Scene scene);

    Optional<String[]> getStoryboardPreview(UUID storyboardId);

    boolean updateUsageHistory(UUID storyboardId, UUID memberId, String status);

    void saveUsageHistory(UUID storyboardId, UUID memberId, StoryboardUsageStatus status);

    Optional<List<Topic>> findTopicsOfStoryboard(UUID storyboardId);
}
