package com.orv.storyboard.repository.jdbc;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.orv.storyboard.domain.Scene;
import com.orv.storyboard.domain.Storyboard;
import com.orv.storyboard.domain.StoryboardPreview;
import com.orv.storyboard.domain.StoryboardUsageStatus;
import com.orv.storyboard.domain.Topic;

import java.util.*;
import com.orv.storyboard.repository.StoryboardRepository;
@Repository
@Slf4j
public class JdbcStoryboardRepository implements StoryboardRepository {
    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert simpleJdbcInsertStoryboard;
    private final SimpleJdbcInsert simpleJdbcInsertScene;

    public JdbcStoryboardRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.simpleJdbcInsertStoryboard = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("storyboard")
                .usingColumns("title", "start_scene_id")
                .usingGeneratedKeyColumns("id");

        this.simpleJdbcInsertScene = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("scene")
                .usingColumns("name", "scene_type", "content", "storyboard_id")
                .usingGeneratedKeyColumns("id");
    }

    @Override
    public Optional<Storyboard> findById(UUID id) {
        String sql = """
                SELECT id, title, start_scene_id, status, participation_count, max_participation_limit
                FROM storyboard WHERE id = ? AND status != 'DELETED'
                """;

        try {
            Storyboard storyboard = jdbcTemplate.queryForObject(sql, new Object[]{id}, new StoryboardRowMapper());
            return Optional.of(storyboard);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Scene> findSceneById(UUID id) {
        String sql = """
                SELECT s.id, s.name, s.scene_type, s.content, s.storyboard_id
                FROM scene s
                JOIN storyboard sb ON s.storyboard_id = sb.id
                WHERE s.id = ? AND sb.status != 'DELETED'
                """;

        try {
            Scene scene = jdbcTemplate.queryForObject(sql, new Object[]{id}, new BeanPropertyRowMapper<>(Scene.class));
            return Optional.of(scene);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<List<Scene>> findScenesByStoryboardId(UUID id) {
        String sql = """
                SELECT s.id, s.name, s.scene_type, s.content, s.storyboard_id
                FROM scene s
                JOIN storyboard sb ON s.storyboard_id = sb.id
                WHERE s.storyboard_id = ? AND sb.status != 'DELETED'
                """;

        try {
            List<Scene> scenes = jdbcTemplate.query(sql, new Object[]{id}, new BeanPropertyRowMapper<>(Scene.class));
            return Optional.of(scenes);
        } catch (Exception e) {
            log.error("findScenesByStoryboardId failed id={}", id, e);
            return Optional.empty();
        }
    }

    @Override
    public Storyboard save(Storyboard storyboard) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("title", storyboard.getTitle());
        parameters.put("start_scene_id", storyboard.getStartSceneId());

        KeyHolder keyHolder = (KeyHolder) simpleJdbcInsertStoryboard.executeAndReturnKeyHolder(new MapSqlParameterSource(parameters));
        Map<String, Object> keys = keyHolder.getKeys();

        if (keys != null && keys.containsKey("id")) {
            UUID generatedId = (UUID) keys.get("id");
            storyboard.setId(generatedId);
        }

        return storyboard;
    }

    @Override
    public Scene saveScene(Scene scene) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", scene.getName());
        parameters.put("scene_type", scene.getSceneType());
        parameters.put("content", scene.getContent());
        parameters.put("storyboard_id", scene.getStoryboardId());

        KeyHolder keyHolder = (KeyHolder) simpleJdbcInsertScene.executeAndReturnKeyHolder(new MapSqlParameterSource(parameters));
        Map<String, Object> keys = keyHolder.getKeys();

        if (keys != null && keys.containsKey("id")) {
            UUID generatedId = (UUID) keys.get("id");
            scene.setId(generatedId);
        }

        return scene;
    }

    @Override
    public Optional<String[]> getStoryboardPreview(UUID storyboardId) {
        String sql = """
                SELECT sp.storyboard_id, sp.examples
                FROM storyboard_preview sp
                JOIN storyboard sb ON sp.storyboard_id = sb.id
                WHERE sp.storyboard_id = ? AND sb.status != 'DELETED'
                """;

        try {
            StoryboardPreview storyboardPreview = jdbcTemplate.queryForObject(sql, new Object[]{storyboardId}, new BeanPropertyRowMapper<>(StoryboardPreview.class));
            return Optional.of((String[]) storyboardPreview.getExamples().getArray());
        } catch (Exception e) {
            log.error("getStoryboardPreview failed storyboardId={}", storyboardId, e);
            return Optional.empty();
        }
    }

    @Override
    public boolean updateUsageHistory(UUID storyboardId, UUID memberId, String status) {
        // 단일 쿼리로 UPDATE 또는 INSERT 수행 (race condition 방지)
        // CTE를 사용하여 먼저 UPDATE를 시도하고, UPDATE된 행이 없으면 INSERT
        String sql = "WITH updated AS ( " +
                "    UPDATE storyboard_usage_history " +
                "    SET updated_at = CURRENT_TIMESTAMP, status = ? " +
                "    WHERE storyboard_id = ? AND member_id = ? " +
                "      AND status <> 'COMPLETED' " +
                "      AND created_at >= (CURRENT_TIMESTAMP - INTERVAL '1 hour') " +
                "    RETURNING id " +
                "), inserted AS ( " +
                "    INSERT INTO storyboard_usage_history (storyboard_id, member_id, status) " +
                "    SELECT ?, ?, ? " +
                "    WHERE NOT EXISTS (SELECT 1 FROM updated) " +
                "    RETURNING id " +
                ") " +
                "SELECT COUNT(*) as affected FROM ( " +
                "    SELECT id FROM updated " +
                "    UNION ALL " +
                "    SELECT id FROM inserted " +
                ") as result";

        Integer affectedRows = jdbcTemplate.queryForObject(sql, Integer.class,
                status, storyboardId, memberId,  // UPDATE parameters
                storyboardId, memberId, status   // INSERT parameters
        );

        return affectedRows != null && affectedRows > 0;
    }

    @Override
    public Optional<List<Topic>> findTopicsOfStoryboard(UUID storyboardId) {
        String sql = """
                SELECT t.id, t.name, t.description, t.thumbnail_url,
                COALESCE(json_agg(json_build_object('name', h.name, 'color', h.color))
                FILTER (WHERE h.id IS NOT NULL), '[]') AS hashtags
                FROM topic t
                JOIN storyboard_topic st ON t.id = st.topic_id
                JOIN storyboard sb ON st.storyboard_id = sb.id
                LEFT JOIN hashtag_topic ht ON t.id = ht.topic_id
                LEFT JOIN hashtag h ON h.id = ht.hashtag_id
                WHERE st.storyboard_id = ? AND sb.status != 'DELETED'
                GROUP BY t.id, t.name, t.description, t.thumbnail_url
                """;

        try {
            List<Topic> topics = jdbcTemplate.query(sql, new Object[]{storyboardId}, new TopicRowMapper());
            return Optional.of(topics);
        } catch (Exception e) {
            log.error("findTopicsOfStoryboard failed storyboardId={}", storyboardId, e);
            return Optional.empty();
        }
    }

    @Override
    public void saveUsageHistory(UUID storyboardId, UUID memberId, StoryboardUsageStatus status) {
        String sql = "INSERT INTO storyboard_usage_history (storyboard_id, member_id, status) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, storyboardId, memberId, status.getValue());
    }

    @Override
    public int incrementParticipationCountSafely(UUID id) {
        String sql = """
            UPDATE storyboard SET participation_count = participation_count + 1
            WHERE id = ?
                AND (max_participation_limit IS NULL OR participation_count < max_participation_limit)
                AND status = 'ACTIVE';
            """;
        int affectedRows = jdbcTemplate.update(sql, id);
        return affectedRows;
    }
}
