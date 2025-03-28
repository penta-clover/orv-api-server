package com.orv.api.domain.storyboard;

import com.orv.api.domain.storyboard.dto.Scene;
import com.orv.api.domain.storyboard.dto.Storyboard;
import com.orv.api.domain.storyboard.dto.StoryboardPreview;
import com.orv.api.domain.storyboard.dto.Topic;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.*;

@Repository
@Slf4j
public class JdbcStoryboardRepository implements StoryboardRepository {
    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert simpleJdbcInsertStoryboard;
    private final SimpleJdbcInsert simpleJdbcInsertScene;
    private final SimpleJdbcInsert simpleJdbcInsertUsageHistory;

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

        this.simpleJdbcInsertUsageHistory = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("storyboard_usage_history")
                .usingColumns("storyboard_id", "member_id", "status")
                .usingGeneratedKeyColumns("id");
    }

    @Override
    public Optional<Storyboard> findById(UUID id) {
        String sql = "SELECT id, title, start_scene_id FROM storyboard WHERE id = ?";

        try {
            Storyboard storyboard = jdbcTemplate.queryForObject(sql, new Object[]{id}, new BeanPropertyRowMapper<>(Storyboard.class));
            return Optional.of(storyboard);
        } catch (EmptyResultDataAccessException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @Override
    public Optional<Scene> findSceneById(UUID id) {
        String sql = "SELECT id, name, scene_type, content, storyboard_id FROM scene WHERE id = ?";

        try {
            Scene scene = jdbcTemplate.queryForObject(sql, new Object[]{id}, new BeanPropertyRowMapper<>(Scene.class));
            return Optional.of(scene);
        } catch (EmptyResultDataAccessException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @Override
    public Optional<List<Scene>> findScenesByStoryboardId(UUID id) {
        String sql = "SELECT id, name, scene_type, content, storyboard_id FROM scene WHERE storyboard_id = ?";

        try {
            List<Scene> scenes = jdbcTemplate.query(sql, new Object[]{id}, new BeanPropertyRowMapper<>(Scene.class));
            return Optional.of(scenes);
        } catch (Exception e) {
            e.printStackTrace();
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
        String sql = "SELECT storyboard_id, examples FROM storyboard_preview WHERE storyboard_id = ?";

        try {
            StoryboardPreview storyboardPreview = jdbcTemplate.queryForObject(sql, new Object[]{storyboardId}, new BeanPropertyRowMapper<>(StoryboardPreview.class));
            return Optional.of((String[]) storyboardPreview.getExamples().getArray());
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @Override
    public boolean updateUsageHistory(UUID storyboardId, UUID memberId, String status) {
        // 조건에 맞는 기존 레코드 조회
        String selectSql = "SELECT id FROM storyboard_usage_history " +
                "WHERE storyboard_id = ? AND member_id = ? " +
                "  AND status <> 'COMPLETED' " +
                "  AND created_at >= (CURRENT_TIMESTAMP - INTERVAL '1 hour') " +
                "LIMIT 1";

        List<UUID> existingIds = jdbcTemplate.query(selectSql, new Object[]{storyboardId, memberId},
                (rs, rowNum) -> (UUID) rs.getObject("id"));

        if (!existingIds.isEmpty()) {
            // 조건에 맞는 레코드가 존재하면 UPDATE
            UUID recordId = existingIds.get(0);
            String updateSql = "UPDATE storyboard_usage_history " +
                    "SET updated_at = CURRENT_TIMESTAMP, status = ? " +
                    "WHERE id = ?";
            int updatedRows = jdbcTemplate.update(updateSql, status, recordId);
            return updatedRows > 0;
        } else {
            // 해당 조건의 레코드가 없으면 INSERT
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("storyboard_id", storyboardId);
            parameters.put("member_id", memberId);
            parameters.put("status", status);

            // updated_at, created_at은 DEFAULT로 CURRENT_TIMESTAMP가 들어감
            KeyHolder keyHolder = simpleJdbcInsertUsageHistory.executeAndReturnKeyHolder(new MapSqlParameterSource(parameters));
            return keyHolder.getKeys() != null;
        }
    }

    @Override
    public Optional<List<Topic>> findTopicsOfStoryboard(UUID storyboardId) {
        String sql = "SELECT t.id, t.name, t.description " +
                "FROM topic t " +
                "JOIN storyboard_topic st ON t.id = st.topic_id " +
                "WHERE st.storyboard_id = ?";

        try {
            List<Topic> topics = jdbcTemplate.query(sql, new Object[]{storyboardId}, new BeanPropertyRowMapper<>(Topic.class));
            return Optional.of(topics);
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }
}
