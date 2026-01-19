package com.orv.api.domain.storyboard.controller;

import com.orv.api.domain.storyboard.controller.dto.StoryboardPreviewResponse;
import com.orv.api.domain.storyboard.service.StoryboardService;
import com.orv.api.domain.storyboard.service.dto.*;
import com.orv.api.global.dto.ApiResponse;
import com.orv.api.global.dto.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final StoryboardService storyboardService;

    @GetMapping("/{storyboardId}")
    public ApiResponse getStoryboard(@PathVariable String storyboardId) {
        Optional<Storyboard> foundStoryboard = storyboardService.getStoryboard(UUID.fromString(storyboardId));

        if (foundStoryboard.isEmpty()) {
            return ApiResponse.fail(ErrorCode.NOT_FOUND, 404);
        }

        return ApiResponse.success(foundStoryboard.get(), 200);
    }

    @GetMapping("/{storyboardId}/scene/all")
    public ApiResponse getAllScene(@PathVariable String storyboardId) {
        try {
            UUID storyboardUUID = UUID.fromString(storyboardId);
            Optional<List<Scene>> scenesOrEmpty = storyboardService.getAllScenes(storyboardUUID);

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
        String memberId = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<Scene> foundScene = storyboardService.getSceneAndUpdateUsageHistory(
                UUID.fromString(sceneId),
                UUID.fromString(memberId)
        );

        if (foundScene.isEmpty()) {
            return ApiResponse.fail(ErrorCode.NOT_FOUND, 404);
        }

        return ApiResponse.success(foundScene.get(), 200);
    }

    @GetMapping("/{storyboardId}/preview")
    public ApiResponse getStoryboardPreview(@PathVariable String storyboardId) {
        UUID storyboardUUID = UUID.fromString(storyboardId);
        Optional<StoryboardPreviewInfo> previewInfoOrEmpty = storyboardService.getStoryboardPreview(storyboardUUID);

        if (previewInfoOrEmpty.isEmpty()) {
            return ApiResponse.fail(ErrorCode.NOT_FOUND, 404);
        }

        // Convert Service DTO to Controller DTO
        StoryboardPreviewInfo previewInfo = previewInfoOrEmpty.get();
        StoryboardPreviewResponse response = new StoryboardPreviewResponse(
                previewInfo.getStoryboardId(),
                previewInfo.getQuestionCount(),
                previewInfo.getQuestions()
        );

        return ApiResponse.success(response, 200);
    }

    @GetMapping("/{storyboardId}/topic/list")
    public ApiResponse getTopicsOfStoryboard(@PathVariable String storyboardId) {
        UUID storyboardUUID = UUID.fromString(storyboardId);
        Optional<List<Topic>> topicsOrEmpty = storyboardService.getTopicsOfStoryboard(storyboardUUID);

        if (topicsOrEmpty.isEmpty()) {
            return ApiResponse.fail(ErrorCode.UNKNOWN, 500);
        }

        return ApiResponse.success(topicsOrEmpty.get(), 200);
    }
}
