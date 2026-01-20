package com.orv.api.domain.auth.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.orv.api.domain.auth.service.dto.Member;
import com.orv.api.domain.auth.service.dto.Role;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

@Primary
@Repository
@Slf4j
public class JdbcMemberRepository implements MemberRepository {
    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert simpleJdbcInsert;

    public JdbcMemberRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("member")
                .usingColumns("id", "nickname", "provider", "social_id", "email", "profile_image_url", "phone_number", "birthday", "gender", "name");
    }

    @Override
    public Optional<Member> findByProviderAndSocialId(String provider, String socialId) {
        String sql = "SELECT id, nickname, provider, social_id, email, profile_image_url, created_at, phone_number, birthday, gender, name FROM member WHERE provider = ? AND social_id = ?";

        try {
            Member member = jdbcTemplate.queryForObject(sql, new Object[]{provider, socialId}, new BeanPropertyRowMapper<>(Member.class));
            return Optional.of(member);
        } catch (EmptyResultDataAccessException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @Override
    public Optional<Member> findByNickname(String nickname) {
        String sql = "SELECT id, nickname, provider, social_id, email, profile_image_url, created_at, phone_number, birthday, gender, name FROM member WHERE nickname = ?";

        try {
            Member member = jdbcTemplate.queryForObject(sql, new Object[]{nickname}, new BeanPropertyRowMapper<>(Member.class));
            return Optional.of(member);
        } catch (EmptyResultDataAccessException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @Override
    public Optional<List<Role>> findRolesById(UUID memberId) {
        String sql = "SELECT r.id, r.name " +
                "FROM role r " +
                "JOIN member_role mr ON r.id = mr.role_id " +
                "WHERE mr.member_id = ?";

        try {
            List<Role> roles = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Role.class), memberId);
            log.info("Roles: {}", roles);
            return Optional.of(roles);
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @Override
    public Member save(Member member) {
        Map<String, Object> parameters = new HashMap<>();

        parameters.put("id", member.getId());
        parameters.put("nickname", member.getNickname());
        parameters.put("provider", member.getProvider());
        parameters.put("social_id", member.getSocialId());
        parameters.put("email", member.getEmail());
        parameters.put("profile_image_url", member.getProfileImageUrl());
        parameters.put("phone_number", member.getPhoneNumber());
        parameters.put("birthday", member.getBirthday());
        parameters.put("gender", member.getGender());
        parameters.put("name", member.getName());

        simpleJdbcInsert.execute(parameters);

        return member;
    }

    @Override
    public Optional<Member> findById(UUID memberId) {
        String sql = "SELECT id, nickname, provider, social_id, email, profile_image_url, created_at, phone_number, birthday, gender, name FROM member WHERE id = ?";

        try {
            Member member = jdbcTemplate.queryForObject(sql, new Object[]{memberId}, new BeanPropertyRowMapper<>(Member.class));
            return Optional.of(member);
        } catch (EmptyResultDataAccessException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @Override
    public List<Member> findByProvider(String provider) {
        String sql = "SELECT id, nickname, provider, social_id, email, profile_image_url, created_at, phone_number, birthday, gender, name FROM member WHERE provider = ?";
        return jdbcTemplate.query(sql, new Object[]{provider}, new BeanPropertyRowMapper<>(Member.class));
    }
}
