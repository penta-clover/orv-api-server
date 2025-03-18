package com.orv.api.domain.storyboard;

import com.orv.api.domain.storyboard.dto.Scene;
import com.orv.api.domain.storyboard.dto.Storyboard;

import java.util.Optional;
import java.util.UUID;

public interface StoryboardRepository {
    Optional<Storyboard> findById(UUID id);

    Optional<Scene> findSceneById(UUID id);

    Storyboard save(Storyboard storyboard);

    Scene saveScene(Scene scene);
}
