package com.orv.api.domain.storyboard.controller;

import com.orv.api.domain.storyboard.controller.dto.StoryboardPreviewResponse;
import com.orv.api.domain.storyboard.repository.StoryboardRepository;
import com.orv.api.domain.storyboard.service.dto.*;
import com.orv.api.global.dto.ApiResponse;
import com.orv.api.global.dto.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v0/storyboard")
@RequiredArgsConstructor
public class StoryboardController {
    private final StoryboardRepository storyboardRepository;

    @GetMapping("/{storyboardId}")
    public ApiResponse getStoryboard(@PathVariable String storyboardId) {
        Optional<Storyboard> foundStoryboard = storyboardRepository.findById(UUID.fromString(storyboardId));

        if (foundStoryboard.isEmpty()) {
            return ApiResponse.fail(ErrorCode.NOT_FOUND, 404);
        }

        Storyboard storyboard = foundStoryboard.get();
        return ApiResponse.success(storyboard, 200);
    }

    @GetMapping("/{storyboardId}/scene/all")
    public ApiResponse getAllScene(@PathVariable String storyboardId) {
        try {
            UUID storyboardUUID = UUID.fromString(storyboardId);
            Optional<List<Scene>> scenesOrEmpty = storyboardRepository.findScenesByStoryboardId(storyboardUUID);

            if (scenesOrEmpty.isEmpty()) {
                return ApiResponse.fail(ErrorCode.UNKNOWN, 500);
            }

            List<Scene> scenes = scenesOrEmpty.get();
            return ApiResponse.success(scenes, 200);
        } catch (Exception e) {
            return ApiResponse.fail(ErrorCode.UNKNOWN, 500);
        }
    }

    @GetMapping("/scene/{sceneId}")
    public ApiResponse getScene(@PathVariable String sceneId) {
        String memberId = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<Scene> foundScene = storyboardRepository.findSceneById(UUID.fromString(sceneId));

        if (foundScene.isEmpty()) {
            return ApiResponse.fail(ErrorCode.NOT_FOUND, 404);
        }

        Scene scene = foundScene.get();

        String status = scene.getSceneType().equals("END") ? "COMPLETED" : "STARTED";
        storyboardRepository.updateUsageHistory(scene.getStoryboardId(), UUID.fromString(memberId), status);

        return ApiResponse.success(scene, 200);
    }

    @GetMapping("/{storyboardId}/preview")
    public ApiResponse getStoryboardPreview(@PathVariable String storyboardId) {
        UUID storyboardUUID = UUID.fromString(storyboardId);
        Optional<List<Scene>> scenesOrEmpty = storyboardRepository.findScenesByStoryboardId(storyboardUUID);

        if (scenesOrEmpty.isEmpty()) {
            return ApiResponse.fail(ErrorCode.NOT_FOUND, 404);
        }

        List<Scene> scenes = scenesOrEmpty.get();
        long questionCount = scenes.stream()
                .filter((elem) -> elem.getSceneType().equals("QUESTION"))
                .count();

        Optional<String[]> examplesOrEmpty = storyboardRepository.getStoryboardPreview(storyboardUUID);

        if (examplesOrEmpty.isEmpty()) {
            return ApiResponse.fail(ErrorCode.UNKNOWN, 500);
        }

        String[] examples = examplesOrEmpty.get();

        return ApiResponse.success(new StoryboardPreviewResponse(
                storyboardUUID, Integer.valueOf((int) questionCount), Arrays.stream(examples).toList()), 200);
    }

    @GetMapping("/{storyboardId}/topic/list")
    public ApiResponse getTopicsOfStoryboard(@PathVariable String storyboardId) {
        UUID storyboardUUID = UUID.fromString(storyboardId);
        Optional<List<Topic>> topicsOrEmpty = storyboardRepository.findTopicsOfStoryboard(storyboardUUID);

        if (topicsOrEmpty.isEmpty()) {
            return ApiResponse.fail(ErrorCode.UNKNOWN, 500);
        }

        List<Topic> topics = topicsOrEmpty.get();
        return ApiResponse.success(topics, 200);
    }
}
