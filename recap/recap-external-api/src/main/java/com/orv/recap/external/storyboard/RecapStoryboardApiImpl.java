package com.orv.recap.external.storyboard;

import com.orv.storyboard.repository.StoryboardRepository;
import com.orv.storyboard.domain.Scene;
import com.orv.storyboard.domain.Storyboard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RecapStoryboardApiImpl implements RecapStoryboardApi {
    private final StoryboardRepository storyboardRepository;

    @Override
    public Optional<StoryboardInfo> getStoryboard(UUID storyboardId) {
        return storyboardRepository.findById(storyboardId)
            .map(sb -> new StoryboardInfo(
                sb.getId(),
                sb.getTitle()
            ));
    }

    @Override
    public Optional<List<SceneInfo>> getScenes(UUID storyboardId) {
        return storyboardRepository.findScenesByStoryboardId(storyboardId)
            .map(scenes -> scenes.stream()
                .map(scene -> new SceneInfo(
                    scene.getId(),
                    scene.getStoryboardId(),
                    scene.getName() != null ? scene.getName() : ""
                ))
                .collect(Collectors.toList())
            );
    }
}
