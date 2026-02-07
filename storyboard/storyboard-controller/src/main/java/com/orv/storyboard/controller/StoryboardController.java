package com.orv.storyboard.controller;

import com.orv.storyboard.orchestrator.dto.*;
import com.orv.storyboard.orchestrator.StoryboardOrchestrator;
import com.orv.common.dto.ApiResponse;
import com.orv.common.dto.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v0/storyboard")
@RequiredArgsConstructor
public class StoryboardController {
    private final StoryboardOrchestrator storyboardOrchestrator;

    @GetMapping("/{storyboardId}")
    public ApiResponse getStoryboard(@PathVariable String storyboardId) {
        Optional<StoryboardResponse> foundStoryboard = storyboardOrchestrator.getStoryboard(UUID.fromString(storyboardId));

        if (foundStoryboard.isEmpty()) {
            return ApiResponse.fail(ErrorCode.NOT_FOUND, 404);
        }

        return ApiResponse.success(foundStoryboard.get(), 200);
    }

    @GetMapping("/{storyboardId}/scene/all")
    public ApiResponse getAllScene(@PathVariable String storyboardId) {
        try {
            UUID storyboardUUID = UUID.fromString(storyboardId);
            Optional<List<SceneResponse>> scenesOrEmpty = storyboardOrchestrator.getAllScenes(storyboardUUID);

            if (scenesOrEmpty.isEmpty()) {
                return ApiResponse.fail(ErrorCode.UNKNOWN, 500);
            }

            return ApiResponse.success(scenesOrEmpty.get(), 200);
        } catch (Exception e) {
            return ApiResponse.fail(ErrorCode.UNKNOWN, 500);
        }
    }

    @GetMapping("/scene/{sceneId}")
    public ApiResponse getScene(@PathVariable String sceneId) {
        Optional<SceneResponse> foundScene = storyboardOrchestrator.getScene(UUID.fromString(sceneId));

        if (foundScene.isEmpty()) {
            return ApiResponse.fail(ErrorCode.NOT_FOUND, 404);
        }

        return ApiResponse.success(foundScene.get(), 200);
    }

    @GetMapping("/{storyboardId}/preview")
    public ApiResponse getStoryboardPreview(@PathVariable String storyboardId) {
        UUID storyboardUUID = UUID.fromString(storyboardId);
        Optional<StoryboardPreviewResponse> previewOrEmpty = storyboardOrchestrator.getStoryboardPreview(storyboardUUID);

        if (previewOrEmpty.isEmpty()) {
            return ApiResponse.fail(ErrorCode.NOT_FOUND, 404);
        }

        return ApiResponse.success(previewOrEmpty.get(), 200);
    }

    @GetMapping("/{storyboardId}/topic/list")
    public ApiResponse getTopicsOfStoryboard(@PathVariable String storyboardId) {
        UUID storyboardUUID = UUID.fromString(storyboardId);
        Optional<List<TopicResponse>> topicsOrEmpty = storyboardOrchestrator.getTopicsOfStoryboard(storyboardUUID);

        if (topicsOrEmpty.isEmpty()) {
            return ApiResponse.fail(ErrorCode.UNKNOWN, 500);
        }

        return ApiResponse.success(topicsOrEmpty.get(), 200);
    }
}
