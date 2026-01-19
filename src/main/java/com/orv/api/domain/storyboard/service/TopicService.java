package com.orv.api.domain.storyboard.service;

import com.orv.api.domain.storyboard.repository.TopicRepository;
import com.orv.api.domain.storyboard.service.dto.Storyboard;
import com.orv.api.domain.storyboard.service.dto.Topic;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TopicService {
    private final TopicRepository topicRepository;

    public List<Topic> getTopicsByCategory(String categoryCode) {
        return topicRepository.findTopicsByCategoryCode(categoryCode);
    }

    public Optional<Storyboard> getNextStoryboard(UUID topicId) {
        List<Storyboard> storyboards = topicRepository.findStoryboardsByTopicId(topicId);

        if (storyboards.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(storyboards.get(0));
    }

    public Optional<Topic> getTopic(UUID topicId) {
        return topicRepository.findTopicById(topicId);
    }
}
