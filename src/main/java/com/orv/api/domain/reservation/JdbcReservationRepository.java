package com.orv.api.domain.reservation;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcReservationRepository implements ReservationRepository {
    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert simpleJdbcInsert;

    public JdbcReservationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("interview_reservation")
                .usingColumns("member_id", "storyboard_id", "scheduled_at")
                .usingGeneratedKeyColumns("id", "created_at");
    }

    @Override
    public Optional<UUID> reserveInterview(UUID memberId, UUID storyboardId, LocalDateTime scheduledAt) {
        Map<String, Object> params = new HashMap<>();

        params.put("member_id", memberId);
        params.put("storyboard_id", storyboardId);
        params.put("scheduled_at", scheduledAt);

        KeyHolder keyHolder = (KeyHolder) simpleJdbcInsert.executeAndReturnKeyHolder(new MapSqlParameterSource(params));
        Map<String, Object> keys = keyHolder.getKeys();

        if(keys != null && keys.containsKey("id")) {
            return Optional.of((UUID) keys.get("id"));
        }

        return Optional.of(null);
    }
}
