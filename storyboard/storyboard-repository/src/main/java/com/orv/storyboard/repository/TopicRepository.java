package com.orv.storyboard.repository;

import org.springframework.stereotype.Repository;

import com.orv.storyboard.domain.Storyboard;
import com.orv.storyboard.domain.Topic;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TopicRepository {
    List<Topic> findTopics();
    List<Topic> findTopicsByCategoryCode(String categoryCode);
    List<Storyboard> findStoryboardsByTopicId(UUID topicId);
    Optional<Topic> findTopicById(UUID topicId);
}
