package com.orv.api.domain.storyboard;

import com.orv.api.domain.storyboard.dto.Scene;
import com.orv.api.domain.storyboard.dto.Storyboard;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
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
        String sql = "SELECT id, title, start_scene_id FROM storyboard WHERE id = ?";

        try {
            Storyboard storyboard = jdbcTemplate.queryForObject(sql, new Object[]{id}, new BeanPropertyRowMapper<>(Storyboard.class));
            return Optional.of(storyboard);
        } catch (EmptyResultDataAccessException e) {
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
}
