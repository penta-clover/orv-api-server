package com.orv.api.domain.auth;

import com.orv.api.domain.auth.dto.Member;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Primary
@Repository
public class JdbcMemberRepository implements MemberRepository {
    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert simpleJdbcInsert;

    public JdbcMemberRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("member")
                .usingColumns("nickname", "provider", "social_id", "email", "profile_image_url", "role", "phone_number", "birthday", "gender", "name")
                .usingGeneratedKeyColumns("id");
    }

    @Override
    public Optional<Member> findByProviderAndSocialId(String provider, String socialId) {
        String sql = "SELECT id, nickname, provider, social_id, email, profile_image_url, created_at, role, phone_number, birthday, gender, name FROM member WHERE provider = ? AND social_id = ?";

        try {
            Member member = jdbcTemplate.queryForObject(sql, new Object[]{provider, socialId}, new BeanPropertyRowMapper<>(Member.class));
            return Optional.of(member);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Member> findByNickname(String nickname) {
        String sql = "SELECT id, nickname, provider, social_id, email, profile_image_url, created_at, role, phone_number, birthday, gender, name FROM member WHERE nickname = ?";

        try {
            Member member = jdbcTemplate.queryForObject(sql, new Object[]{nickname}, new BeanPropertyRowMapper<>(Member.class));
            return Optional.of(member);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Member save(Member member) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("nickname", member.getNickname());
        parameters.put("provider", member.getProvider());
        parameters.put("social_id", member.getSocialId());
        parameters.put("email", member.getEmail());
        parameters.put("profile_image_url", member.getProfileImageUrl());
        parameters.put("role", member.getRole());
        parameters.put("phone_number", member.getPhoneNumber());
        parameters.put("birthday", member.getBirthday());
        parameters.put("gender", member.getGender());
        parameters.put("name", member.getName());

        KeyHolder keyHolder = (KeyHolder) simpleJdbcInsert.executeAndReturnKeyHolder(parameters);
        Map<String, Object> keys = keyHolder.getKeys();

        if (keys != null && keys.containsKey("id")) {
            UUID generatedId = (UUID) keys.get("id");
            member.setId(generatedId);
        }

        return member;
    }
}
