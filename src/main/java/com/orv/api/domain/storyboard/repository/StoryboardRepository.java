package com.orv.api.domain.storyboard.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.orv.api.domain.storyboard.service.dto.Scene;
import com.orv.api.domain.storyboard.service.dto.Storyboard;
import com.orv.api.domain.storyboard.service.dto.Topic;

public interface StoryboardRepository {
    Optional<Storyboard> findById(UUID id);

    Optional<Scene> findSceneById(UUID id);

    Optional<List<Scene>> findScenesByStoryboardId(UUID id);

    Storyboard save(Storyboard storyboard);

    Scene saveScene(Scene scene);

    Optional<String[]> getStoryboardPreview(UUID storyboardId);

    boolean updateUsageHistory(UUID storyboardId, UUID memberId, String status);

    Optional<List<Topic>> findTopicsOfStoryboard(UUID storyboardId);
}
