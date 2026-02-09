package com.orv.storyboard.repository.jdbc;

import com.orv.storyboard.domain.Storyboard;
import com.orv.storyboard.domain.StoryboardStatus;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class StoryboardRowMapper implements RowMapper<Storyboard> {
    @Override
    public Storyboard mapRow(ResultSet rs, int rowNum) throws SQLException {
        Storyboard storyboard = new Storyboard();
        storyboard.setId((UUID) rs.getObject("id"));
        storyboard.setTitle(rs.getString("title"));
        storyboard.setStartSceneId((UUID) rs.getObject("start_scene_id"));
        storyboard.setStatus(StoryboardStatus.fromValue(rs.getString("status")));
        return storyboard;
    }
}
