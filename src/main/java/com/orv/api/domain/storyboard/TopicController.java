package com.orv.api.domain.storyboard;

import com.orv.api.domain.storyboard.dto.Storyboard;
import com.orv.api.domain.storyboard.dto.Topic;
import com.orv.api.global.dto.ApiResponse;
import com.orv.api.global.dto.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v0/topic")
@RequiredArgsConstructor
public class TopicController {
    private final TopicRepository topicRepository;

    @GetMapping("/list")
    public ApiResponse getTopics() {
        try {
            List<Topic> topics = topicRepository.findTopics();
            return ApiResponse.success(topics, 200);
        } catch (Exception e) {
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
            return ApiResponse.fail(null, 500);
        }
    }
}
