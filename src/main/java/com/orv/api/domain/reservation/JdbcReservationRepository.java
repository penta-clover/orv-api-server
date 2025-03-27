package com.orv.api.domain.reservation;

import com.orv.api.domain.reservation.dto.InterviewReservation;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;

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

    @Override
    public Optional<List<InterviewReservation>> getReservedInterviews(UUID memberId) {
        // 현재 시간으로부터 이후에 위치한 인터뷰
//        String sql = "SELECT id, member_id, storyboard_id, scheduled_at, created_at FROM interview_reservation WHERE member_id = ? AND scheduled_at >= NOW() ORDER BY scheduled_at ASC LIMIT 100";
        String sql = "SELECT r.id, r.member_id, r.storyboard_id, r.scheduled_at, r.created_at " +
                "FROM interview_reservation r " +
                "WHERE r.member_id = ? " +
                "  AND r.scheduled_at >= timezone('Asia/Seoul', now())" +
                "  AND NOT EXISTS ( " +
                "      SELECT 1 " +
                "      FROM storyboard_usage_history suh " +
                "      WHERE suh.storyboard_id = r.storyboard_id " +
                "        AND suh.member_id = r.member_id " +
                "        AND suh.status = 'completed' " +
                "        AND suh.created_at > r.created_at " +
                "  ) " +
                "ORDER BY r.scheduled_at ASC " +
                "LIMIT 100";

        try {
            List<InterviewReservation> reservations = jdbcTemplate.query(sql, new Object[]{memberId}, new BeanPropertyRowMapper<>(InterviewReservation.class));
            return Optional.of(reservations);
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }
}
