package com.orv.api.domain.storyboard;

import com.orv.api.domain.storyboard.dto.Storyboard;
import com.orv.api.domain.storyboard.dto.Topic;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TopicRepository {
    List<Topic> findTopics();
    List<Storyboard> findStoryboardsByTopicId(UUID topicId);
    Optional<Topic> findTopicById(UUID topicId);
}
