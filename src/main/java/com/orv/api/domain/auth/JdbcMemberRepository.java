package com.orv.api.domain.auth;

import com.orv.api.domain.auth.dto.Member;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
@Primary
@Repository
public class JdbcMemberRepository implements MemberRepository {
    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert simpleJdbcInsert;

    public JdbcMemberRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("member")
                .usingColumns("nickname", "provider", "social_id", "email")
                .usingGeneratedKeyColumns("id");
    }

    @Override
    public Optional<Member> findByProviderAndSocialId(String provider, String socialId) {
        String sql = "SELECT id, nickname, provider, social_id, email FROM member " +
                "WHERE provider = ? AND social_id = ?";

        try {
            Member member = jdbcTemplate.queryForObject(sql, new Object[]{provider, socialId}, new MemberRowMapper());
            return Optional.of(member);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Member> findByNickname(String nickname) {
        String sql = "SELECT id, nickname, provider, social_id, email FROM member " +
                "WHERE nickname = ?";

        try {
            Member member = jdbcTemplate.queryForObject(sql, new Object[]{nickname}, new MemberRowMapper());
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

        KeyHolder keyHolder = simpleJdbcInsert.executeAndReturnKeyHolder(new MapSqlParameterSource(parameters));
        Object key = keyHolder.getKeys().get("id");
        member.setId(key.toString());

        return member;
    }

    private static class MemberRowMapper implements RowMapper<Member> {
        @Override
        public Member mapRow(ResultSet rs, int rowNum) throws SQLException {
            Member member = new Member();
            member.setId(rs.getObject("id").toString());
            member.setNickname(rs.getString("nickname"));
            member.setProvider(rs.getString("provider"));
            member.setSocialId(rs.getString("social_id"));
            member.setEmail(rs.getString("email"));
            return member;
        }
    }
}
