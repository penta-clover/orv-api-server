package com.orv.api.domain.reservation;

import com.orv.api.domain.reservation.dto.InterviewReservation;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
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
    public Optional<List<InterviewReservation>> getReservedInterviews(UUID memberId, OffsetDateTime from) {
        // 특정 시간 이후에 위치한 인터뷰
        String sql = "SELECT r.id, r.member_id, r.storyboard_id, r.scheduled_at, r.created_at " +
                "FROM interview_reservation r " +
                "WHERE r.member_id = ? " +
                "  AND r.scheduled_at >= ?" +
                "  AND r.reservation_status = 'pending'" +
                "ORDER BY r.scheduled_at ASC " +
                "LIMIT 100";

        try {
            List<InterviewReservation> reservations = jdbcTemplate.query(sql, new Object[]{memberId, from}, new BeanPropertyRowMapper<>(InterviewReservation.class));
            return Optional.of(reservations);
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @Override
    public boolean changeInterviewReservationStatus(UUID reservationId, String status) {
        String sql = "UPDATE interview_reservation SET reservation_status = ? WHERE id = ?";
        return jdbcTemplate.update(sql, status, reservationId) > 0;
    }

    @Override
    public Optional<InterviewReservation> findInterviewReservationById(UUID reservationId) {
        String sql = "SELECT id, storyboard_id, member_id, scheduled_at, created_at, reservation_status " +
                "FROM interview_reservation " +
                "WHERE id = ?";

        try {
            InterviewReservation reservation = jdbcTemplate.queryForObject(sql, new Object[]{reservationId}, new BeanPropertyRowMapper<>(InterviewReservation.class));
            return Optional.of(reservation);
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }
}
