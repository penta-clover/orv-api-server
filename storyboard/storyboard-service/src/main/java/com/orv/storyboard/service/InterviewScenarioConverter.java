package com.orv.storyboard.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orv.recap.domain.InterviewScenario;
import com.orv.recap.domain.SceneInfo;
import com.orv.recap.external.storyboard.RecapStoryboardApi;
import com.orv.storyboard.domain.Scene;
import com.orv.storyboard.domain.Storyboard;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class InterviewScenarioConverter {

    private final ObjectMapper objectMapper;

    public InterviewScenario create(Storyboard storyboard, List<Scene> allScenes) {
        List<Scene> sortedScenes = sortScenes(allScenes, storyboard.getStartSceneId());

        List<SceneInfo> sceneInfos = sortedScenes.stream()
                .filter(scene -> "QUESTION".equals(scene.getSceneType()))
                .map(scene -> {
                    try {
                        JsonNode contentNode = objectMapper.readTree(scene.getContent());
                        String question = contentNode.has("question") ? contentNode.get("question").asText() : "";
                        return new SceneInfo(scene.getId().toString(), question);
                    } catch (IOException e) {
                        log.error("Failed to parse scene content for scene ID {}: {}", scene.getId(), e.getMessage());
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        return new InterviewScenario(storyboard.getTitle(), sceneInfos);
    }

    /**
     * Create InterviewScenario from cross-domain API DTOs
     * Used by Recap domain to avoid direct dependency on Storyboard domain
     */
    public InterviewScenario createFromApi(
        RecapStoryboardApi.StoryboardInfo storyboard,
        List<RecapStoryboardApi.SceneInfo> scenes
    ) {
        List<SceneInfo> sceneInfos = scenes.stream()
            .map(scene -> new SceneInfo(
                scene.getId().toString(),
                scene.getQuestion()
            ))
            .collect(Collectors.toList());

        return new InterviewScenario(storyboard.getTitle(), sceneInfos);
    }

    private List<Scene> sortScenes(List<Scene> scenes, UUID startSceneId) {
        Map<UUID, Scene> sceneMap = scenes.stream().collect(Collectors.toMap(Scene::getId, Function.identity()));
        List<Scene> sortedScenes = new ArrayList<>();
        Set<UUID> visitedSceneIds = new HashSet<>();
        UUID currentSceneId = startSceneId;

        while (currentSceneId != null) {
            if (!visitedSceneIds.add(currentSceneId)) {
                throw new IllegalStateException("Circular reference detected in storyboard for scene ID: " + currentSceneId);
            }

            Scene currentScene = sceneMap.get(currentSceneId);
            if (currentScene == null) {
                throw new IllegalStateException("Scene with ID " + currentSceneId + " not found, but it is referenced by a nextSceneId.");
            }
            sortedScenes.add(currentScene);

            if ("END".equals(currentScene.getSceneType())) {
                break;
            }

            try {
                JsonNode contentNode = objectMapper.readTree(currentScene.getContent());
                if (contentNode.has("nextSceneId")) {
                    currentSceneId = UUID.fromString(contentNode.get("nextSceneId").asText());
                } else {
                    currentSceneId = null;
                }
            } catch (IOException e) {
                log.error("Failed to parse scene content for nextSceneId, scene ID {}: {}", currentScene.getId(), e.getMessage());
                currentSceneId = null;
            }
        }
        return sortedScenes;
    }
}
