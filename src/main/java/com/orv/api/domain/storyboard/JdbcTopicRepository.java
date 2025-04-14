package com.orv.api.domain.storyboard;

import com.orv.api.domain.storyboard.dto.Storyboard;
import com.orv.api.domain.storyboard.dto.Topic;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JdbcTopicRepository implements TopicRepository {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<Topic> findTopics() {
        String sql = "SELECT t.id, t.name, t.description, t.thumbnail_url, " +
                "COALESCE(json_agg(json_build_object('name', h.name, 'color', h.color)) " +
                "FILTER (WHERE h.id IS NOT NULL), '[]') AS hashtags " +
                "FROM topic t " +
                "LEFT JOIN hashtag_topic ht ON t.id = ht.topic_id " +
                "LEFT JOIN hashtag h ON h.id = ht.hashtag_id " +
                "GROUP BY t.id, t.name, t.description, t.thumbnail_url";

        try {
            return jdbcTemplate.query(sql, new TopicRowMapper());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    @Override
    public List<Topic> findTopicsByCategoryCode(String categoryCode) {
        String sql = "SELECT t.id,\n" +
                "       t.name,\n" +
                "       t.description,\n" +
                "       t.thumbnail_url,\n" +
                "       COALESCE(\n" +
                "               (json_agg(\n" +
                "                DISTINCT json_build_object('name', h.name, 'color', h.color)::jsonb\n" +
                "                        ) FILTER (WHERE h.id IS NOT NULL))::text,\n" +
                "               '[]'\n" +
                "       )::json AS hashtags\n" +
                "FROM topic t\n" +
                "         JOIN category_topic ct ON t.id = ct.topic_id\n" +
                "         JOIN category c ON ct.category_id = c.id\n" +
                "         LEFT JOIN hashtag_topic ht ON t.id = ht.topic_id\n" +
                "         LEFT JOIN hashtag h ON h.id = ht.hashtag_id\n" +
                "WHERE c.code = ?\n" +
                "GROUP BY t.id, t.name, t.description, t.thumbnail_url;";

        try {
            return jdbcTemplate.query(sql, new Object[]{categoryCode}, new TopicRowMapper());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    @Override
    public List<Storyboard> findStoryboardsByTopicId(UUID topicId) {
        String sql = "SELECT s.id, s.title, s.start_scene_id AS startSceneId " +
                "FROM storyboard s " +
                "JOIN storyboard_topic st ON s.id = st.storyboard_id " +
                "WHERE st.topic_id = ?";

        try {
            return jdbcTemplate.query(sql, new Object[]{topicId}, new BeanPropertyRowMapper<>(Storyboard.class));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    @Override
    public Optional<Topic> findTopicById(UUID topicId) {
        String sql = "SELECT t.id, t.name, t.description, t.thumbnail_url, " +
                "       COALESCE(json_agg(json_build_object('name', h.name, 'color', h.color)) FILTER (WHERE h.id IS NOT NULL), '[]') AS hashtags " +
                "FROM topic t " +
                "LEFT JOIN hashtag_topic ht ON t.id = ht.topic_id " +
                "LEFT JOIN hashtag h ON h.id = ht.hashtag_id " +
                "WHERE t.id = ? " +
                "GROUP BY t.id, t.name, t.description, t.thumbnail_url";
        try {
            Topic topic = jdbcTemplate.queryForObject(sql, new Object[]{topicId}, new TopicRowMapper());
            return Optional.of(topic);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }
}
