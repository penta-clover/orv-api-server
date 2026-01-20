package com.orv.api.domain.storyboard.orchestrator;

import com.orv.api.domain.storyboard.controller.dto.HashtagResponse;
import com.orv.api.domain.storyboard.controller.dto.StoryboardResponse;
import com.orv.api.domain.storyboard.controller.dto.TopicResponse;
import com.orv.api.domain.storyboard.service.TopicService;
import com.orv.api.domain.storyboard.service.dto.Storyboard;
import com.orv.api.domain.storyboard.service.dto.Topic;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TopicOrchestrator {
    private final TopicService topicService;

    public List<TopicResponse> getTopicsByCategory(String categoryCode) {
        List<Topic> topics = topicService.getTopicsByCategory(categoryCode);
        return topics.stream()
                .map(this::toTopicResponse)
                .collect(Collectors.toList());
    }

    public Optional<StoryboardResponse> getNextStoryboard(UUID topicId) {
        return topicService.getNextStoryboard(topicId)
                .map(this::toStoryboardResponse);
    }

    public Optional<TopicResponse> getTopic(UUID topicId) {
        return topicService.getTopic(topicId)
                .map(this::toTopicResponse);
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

    private StoryboardResponse toStoryboardResponse(Storyboard storyboard) {
        return new StoryboardResponse(
                storyboard.getId(),
                storyboard.getTitle(),
                storyboard.getStartSceneId()
        );
    }
}
