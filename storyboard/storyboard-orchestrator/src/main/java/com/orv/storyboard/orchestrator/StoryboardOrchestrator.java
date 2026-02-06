package com.orv.storyboard.orchestrator;

import com.orv.storyboard.orchestrator.dto.*;
import com.orv.storyboard.service.StoryboardService;
import com.orv.storyboard.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class StoryboardOrchestrator {
    private final StoryboardService storyboardService;

    public Optional<StoryboardResponse> getStoryboard(UUID storyboardId) {
        return storyboardService.getStoryboard(storyboardId)
                .map(this::toStoryboardResponse);
    }

    public Optional<List<SceneResponse>> getAllScenes(UUID storyboardId) {
        return storyboardService.getAllScenes(storyboardId)
                .map(scenes -> scenes.stream()
                        .map(this::toSceneResponse)
                        .collect(Collectors.toList()));
    }

    public Optional<SceneResponse> getScene(UUID sceneId) {
        return storyboardService.getScene(sceneId)
                .map(this::toSceneResponse);
    }

    public Optional<StoryboardPreviewResponse> getStoryboardPreview(UUID storyboardId) {
        return storyboardService.getStoryboardPreview(storyboardId)
                .map(this::toStoryboardPreviewResponse);
    }

    public Optional<List<TopicResponse>> getTopicsOfStoryboard(UUID storyboardId) {
        return storyboardService.getTopicsOfStoryboard(storyboardId)
                .map(topics -> topics.stream()
                        .map(this::toTopicResponse)
                        .collect(Collectors.toList()));
    }

    private StoryboardResponse toStoryboardResponse(Storyboard storyboard) {
        return new StoryboardResponse(
                storyboard.getId(),
                storyboard.getTitle(),
                storyboard.getStartSceneId()
        );
    }

    private SceneResponse toSceneResponse(Scene scene) {
        return new SceneResponse(
                scene.getId(),
                scene.getName(),
                scene.getSceneType(),
                scene.getContent(),
                scene.getStoryboardId()
        );
    }

    private StoryboardPreviewResponse toStoryboardPreviewResponse(StoryboardPreviewInfo info) {
        return new StoryboardPreviewResponse(
                info.getStoryboardId(),
                info.getQuestionCount(),
                info.getQuestions()
        );
    }

    private TopicResponse toTopicResponse(Topic topic) {
        List<HashtagResponse> hashtags = topic.getHashtags() != null
                ? topic.getHashtags().stream()
                        .map(h -> new HashtagResponse(h.getName(), h.getColor()))
                        .collect(Collectors.toList())
                : null;

        return new TopicResponse(
                topic.getId(),
                topic.getName(),
                topic.getDescription(),
                topic.getThumbnailUrl(),
                hashtags
        );
    }
}
