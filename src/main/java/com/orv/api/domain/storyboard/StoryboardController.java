package com.orv.api.domain.storyboard;

import com.orv.api.domain.storyboard.dto.Scene;
import com.orv.api.domain.storyboard.dto.Storyboard;
import com.orv.api.global.dto.ApiResponse;
import com.orv.api.global.dto.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/scene/{sceneId}")
    public ApiResponse getScene(@PathVariable String sceneId) {
        Optional<Scene> foundScene = storyboardRepository.findSceneById(UUID.fromString(sceneId));

        if (foundScene.isEmpty()) {
            return ApiResponse.fail(ErrorCode.NOT_FOUND, 404);
        }

        Scene scene = foundScene.get();
        return ApiResponse.success(scene, 200);
    }
}
