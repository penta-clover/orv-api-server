package com.orv.reservation.repository.jdbc;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.orv.reservation.domain.ReservationStatus;
import com.orv.reservation.domain.InterviewReservation;
import com.orv.reservation.repository.InterviewReservationRepository;
@Repository
public class JdbcInterviewReservationRepository implements InterviewReservationRepository {
    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert simpleJdbcInsert;

    public JdbcInterviewReservationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("interview_reservation")
                .usingColumns("member_id", "storyboard_id", "scheduled_at", "reservation_status")
                .usingGeneratedKeyColumns("id", "created_at");
    }

    @Override
    public Optional<UUID> reserveInterview(UUID memberId, UUID storyboardId, LocalDateTime scheduledAt) {
        return reserveInterview(memberId, storyboardId, scheduledAt, ReservationStatus.PENDING);
    }

    @Override
    public Optional<UUID> reserveInterview(UUID memberId, UUID storyboardId, LocalDateTime scheduledAt, ReservationStatus status) {
        Map<String, Object> params = new HashMap<>();

        params.put("member_id", memberId);
        params.put("storyboard_id", storyboardId);
        params.put("scheduled_at", scheduledAt);
        params.put("reservation_status", status.getValue());

        KeyHolder keyHolder = (KeyHolder) simpleJdbcInsert.executeAndReturnKeyHolder(new MapSqlParameterSource(params));
        Map<String, Object> keys = keyHolder.getKeys();

        if(keys != null && keys.containsKey("id")) {
            return Optional.of((UUID) keys.get("id"));
        }

        return Optional.empty();
    }

    @Override
    public Optional<List<InterviewReservation>> getReservedInterviews(UUID memberId, OffsetDateTime from) {
        String sql = """
                SELECT r.id, r.member_id, r.storyboard_id, r.scheduled_at, r.created_at
                FROM interview_reservation r
                JOIN storyboard sb ON r.storyboard_id = sb.id
                WHERE r.member_id = ?
                  AND r.scheduled_at >= ?
                  AND r.reservation_status = 'pending'
                  AND sb.status != 'DELETED'
                ORDER BY r.scheduled_at ASC
                LIMIT 100
                """;

        try {
            List<InterviewReservation> reservations = jdbcTemplate.query(sql, new Object[]{memberId, from}, new BeanPropertyRowMapper<>(InterviewReservation.class));
            return Optional.of(reservations);
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @Override
    public boolean changeInterviewReservationStatus(UUID reservationId, ReservationStatus status) {
        String sql = "UPDATE interview_reservation SET reservation_status = ? WHERE id = ?";
        return jdbcTemplate.update(sql, status.getValue(), reservationId) > 0;
    }

    @Override
    public Optional<InterviewReservation> findInterviewReservationById(UUID reservationId) {
        String sql = """
                SELECT id, storyboard_id, member_id, scheduled_at, created_at, reservation_status, is_used
                FROM interview_reservation
                WHERE id = ?
                """;

        try {
            InterviewReservation reservation = jdbcTemplate.queryForObject(sql, new Object[]{reservationId}, new BeanPropertyRowMapper<>(InterviewReservation.class));
            return Optional.of(reservation);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<InterviewReservation> findInterviewReservationByIdForUpdate(UUID reservationId) {
        String sql = """
                SELECT id, storyboard_id, member_id, scheduled_at, created_at, reservation_status, is_used
                FROM interview_reservation
                WHERE id = ? FOR UPDATE
                """;

        try {
            InterviewReservation reservation = jdbcTemplate.queryForObject(sql, new Object[]{reservationId}, new BeanPropertyRowMapper<>(InterviewReservation.class));
            return Optional.of(reservation);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public int countActiveReservations(UUID memberId, LocalDateTime startAt, LocalDateTime endAt) {
        String sql = "SELECT COUNT(*) FROM interview_reservation " +
                "WHERE member_id = ? " +
                "  AND scheduled_at >= ? " +
                "  AND scheduled_at < ? " +
                "  AND reservation_status IN ('pending', 'done')";

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, memberId, startAt, endAt);
        return count != null ? count : 0;
    }

    @Override
    public boolean markAsUsed(UUID reservationId) {
        String sql = "UPDATE interview_reservation SET is_used = TRUE WHERE id = ? AND is_used = FALSE";
        return jdbcTemplate.update(sql, reservationId) > 0;
    }
}
