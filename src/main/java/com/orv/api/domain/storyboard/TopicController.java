package com.orv.api.domain.storyboard;

import com.orv.api.domain.storyboard.dto.Storyboard;
import com.orv.api.domain.storyboard.dto.Topic;
import com.orv.api.global.dto.ApiResponse;
import com.orv.api.global.dto.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v0/topic")
@RequiredArgsConstructor
public class TopicController {
    private final TopicRepository topicRepository;

    @GetMapping("/list")
    public ApiResponse getTopics(@RequestParam(name = "category", required = false, defaultValue = "DEFAULT") String categoryCode) {
        try {
            List<Topic> topics = topicRepository.findTopicsByCategoryCode(categoryCode);
            return ApiResponse.success(topics, 200);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.fail(null, 500);
        }
    }

    @GetMapping("/{topicId}/storyboard/next")
    public ApiResponse getNextStoryboard(@PathVariable("topicId") String topicId) {
        try {
            List<Storyboard> storyboards = topicRepository.findStoryboardsByTopicId(UUID.fromString(topicId));

            if (storyboards.size() < 1) {
                return ApiResponse.fail(ErrorCode.NOT_FOUND, 404);
            }

            return ApiResponse.success(storyboards.get(0), 200);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.fail(null, 500);
        }
    }

    @GetMapping("/{topicId}")
    public ApiResponse getTopic(@PathVariable("topicId") String topicId) {
        try {
            Optional<Topic> topicOrEmpty = topicRepository.findTopicById(UUID.fromString(topicId));

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
