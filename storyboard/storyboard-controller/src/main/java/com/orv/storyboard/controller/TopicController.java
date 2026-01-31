package com.orv.storyboard.controller;

import com.orv.storyboard.orchestrator.dto.StoryboardResponse;
import com.orv.storyboard.orchestrator.dto.TopicResponse;
import com.orv.storyboard.orchestrator.TopicOrchestrator;
import com.orv.common.dto.ApiResponse;
import com.orv.common.dto.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v0/topic")
@RequiredArgsConstructor
public class TopicController {
    private final TopicOrchestrator topicOrchestrator;

    @GetMapping("/list")
    public ApiResponse getTopics(@RequestParam(name = "category", required = false, defaultValue = "DEFAULT") String categoryCode) {
        try {
            List<TopicResponse> topics = topicOrchestrator.getTopicsByCategory(categoryCode);
            return ApiResponse.success(topics, 200);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.fail(null, 500);
        }
    }

    @GetMapping("/{topicId}/storyboard/next")
    public ApiResponse getNextStoryboard(@PathVariable("topicId") String topicId) {
        try {
            Optional<StoryboardResponse> storyboardOrEmpty = topicOrchestrator.getNextStoryboard(UUID.fromString(topicId));

            if (storyboardOrEmpty.isEmpty()) {
                return ApiResponse.fail(ErrorCode.NOT_FOUND, 404);
            }

            return ApiResponse.success(storyboardOrEmpty.get(), 200);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.fail(null, 500);
        }
    }

    @GetMapping("/{topicId}")
    public ApiResponse getTopic(@PathVariable("topicId") String topicId) {
        try {
            Optional<TopicResponse> topicOrEmpty = topicOrchestrator.getTopic(UUID.fromString(topicId));

            if (topicOrEmpty.isEmpty()) {
                return ApiResponse.fail(ErrorCode.NOT_FOUND, 404);
            }

            return ApiResponse.success(topicOrEmpty.get(), 200);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.fail(null, 500);
        }
    }
}
