package com.orv.api.domain.storyboard;

import com.orv.api.domain.storyboard.dto.Storyboard;
import com.orv.api.domain.storyboard.dto.Topic;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JdbcTopicRepository implements TopicRepository {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<Topic> findTopics() {
        String sql = "SELECT id, name, description, thumbnail_url FROM topic";

        try {
            List<Topic> topics = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Topic.class));
            return topics;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    @Override
    public List<Storyboard> findStoryboardsByTopicId(UUID topicId) {
        String sql = "SELECT s.id, s.title, s.start_scene_id AS startSceneId " + "FROM storyboard s " + "JOIN storyboard_topic st ON s.id = st.storyboard_id " + "WHERE st.topic_id = ?";

        try {
            return jdbcTemplate.query(sql, new Object[]{topicId}, new BeanPropertyRowMapper<>(Storyboard.class));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }
}
