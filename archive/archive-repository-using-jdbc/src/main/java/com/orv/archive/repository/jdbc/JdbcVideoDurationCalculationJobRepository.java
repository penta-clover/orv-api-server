package com.orv.archive.repository.jdbc;

import com.orv.archive.domain.JobStatus;
import com.orv.archive.domain.VideoDurationCalculationJob;
import com.orv.archive.repository.VideoDurationCalculationJobRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
@Slf4j
public class JdbcVideoDurationCalculationJobRepository implements VideoDurationCalculationJobRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcVideoDurationCalculationJobRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void create(UUID videoId) {
        String sql = "INSERT INTO video_duration_extraction_job (video_id, status) VALUES (?, 'PENDING')";
        jdbcTemplate.update(sql, videoId);
    }

    @Override
    @Transactional
    public Optional<VideoDurationCalculationJob> claimNext(Duration stuckThreshold) {
        try {
            LocalDateTime now = LocalDateTime.now();

            // 1. PENDING이거나, PROCESSING이지만 started_at이 stuckThreshold 이상 지난 것 조회
            String selectSql = "SELECT id, video_id, status, created_at, started_at " +
                    "FROM video_duration_extraction_job " +
                    "WHERE status = 'PENDING' " +
                    "   OR (status = 'PROCESSING' AND started_at < ?) " +
                    "ORDER BY id " +
                    "LIMIT 1 " +
                    "FOR UPDATE SKIP LOCKED";

            LocalDateTime stuckThresholdTime = now.minus(stuckThreshold);
            VideoDurationCalculationJob job = jdbcTemplate.queryForObject(
                    selectSql,
                    new VideoDurationCalculationJobRowMapper(),
                    Timestamp.valueOf(stuckThresholdTime)
            );

            if (job == null) {
                return Optional.empty();
            }

            // 2. PROCESSING 상태로 변경 및 started_at 업데이트
            String updateSql = "UPDATE video_duration_extraction_job " +
                    "SET status = 'PROCESSING', started_at = ? " +
                    "WHERE id = ?";
            jdbcTemplate.update(updateSql, Timestamp.valueOf(now), job.getId());

            // 3. 업데이트된 상태로 반환
            job.setStatus(JobStatus.PROCESSING);
            job.setStartedAt(now);

            return Optional.of(job);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public void markCompleted(Long jobId) {
        String sql = "UPDATE video_duration_extraction_job SET status = 'COMPLETED' WHERE id = ?";
        jdbcTemplate.update(sql, jobId);
    }

    @Override
    public void markFailed(Long jobId) {
        String sql = "UPDATE video_duration_extraction_job SET status = 'FAILED' WHERE id = ?";
        jdbcTemplate.update(sql, jobId);
    }

    private static class VideoDurationCalculationJobRowMapper implements RowMapper<VideoDurationCalculationJob> {
        @Override
        public VideoDurationCalculationJob mapRow(ResultSet rs, int rowNum) throws SQLException {
            VideoDurationCalculationJob job = new VideoDurationCalculationJob();
            job.setId(rs.getLong("id"));
            job.setVideoId(UUID.fromString(rs.getString("video_id")));
            job.setStatus(JobStatus.valueOf(rs.getString("status")));

            Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                job.setCreatedAt(createdAt.toLocalDateTime());
            }

            Timestamp startedAt = rs.getTimestamp("started_at");
            if (startedAt != null) {
                job.setStartedAt(startedAt.toLocalDateTime());
            }

            return job;
        }
    }
}
