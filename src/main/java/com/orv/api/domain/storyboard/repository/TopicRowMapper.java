package com.orv.api.domain.storyboard.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orv.api.domain.storyboard.service.dto.Hashtag;
import com.orv.api.domain.storyboard.service.dto.Topic;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TopicRowMapper implements RowMapper<Topic> {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Topic mapRow(ResultSet rs, int rowNum) throws SQLException {
        Topic topic = new Topic();
        topic.setId((UUID) rs.getObject("id"));
        topic.setName(rs.getString("name"));
        topic.setDescription(rs.getString("description"));
        topic.setThumbnailUrl(rs.getString("thumbnail_url"));
        String hashtagsJson = rs.getString("hashtags");
        try {
            List<Hashtag> hashtags = objectMapper.readValue(hashtagsJson, new TypeReference<List<Hashtag>>() {});
            topic.setHashtags(hashtags);
        } catch (Exception e) {
            // 변환 실패시 빈 리스트 할당
            topic.setHashtags(new ArrayList<>());
        }
        return topic;
    }
}