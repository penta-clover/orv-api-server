package com.orv.archive.repository.jdbc;

import com.orv.archive.domain.ClaimedArchiveJob;
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
import java.sql.Types;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
@Slf4j
public class JdbcVideoDurationCalculationJobRepository implements VideoDurationCalculationJobRepository {

    private static final String SELECT_CLAIMABLE_JOB_SQL = """
            SELECT id, video_id, status, created_at, started_at
            FROM video_duration_extraction_job
            WHERE status = 'PENDING'
               OR (status = 'PROCESSING' AND started_at < ?)
            ORDER BY id
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """;

    private static final String UPDATE_CLAIMED_STATUS_SQL = """
            UPDATE video_duration_extraction_job
            SET status = CASE status
                            WHEN 'PENDING' THEN 'PROCESSING'
                            WHEN 'PROCESSING' THEN 'RETRYING'
                         END,
                started_at = ?
            WHERE id = ?
            """;

    private static final String INSERT_JOB_SQL =
            "INSERT INTO video_duration_extraction_job (video_id, status) VALUES (?, 'PENDING')";

    private static final String UPDATE_TO_COMPLETED_SQL =
            "UPDATE video_duration_extraction_job SET status = 'COMPLETED' WHERE id = ? AND status IN ('PROCESSING', 'RETRYING')";

    private static final String UPDATE_TO_FAILED_SQL =
            "UPDATE video_duration_extraction_job SET status = 'FAILED' WHERE id = ? AND status IN ('PROCESSING', 'RETRYING')";

    private static final String RESET_TO_PRE_CLAIM_SQL =
            "UPDATE video_duration_extraction_job SET status = ?, started_at = ? WHERE id = ? AND status IN ('PROCESSING', 'RETRYING')";

    private final JdbcTemplate jdbcTemplate;

    public JdbcVideoDurationCalculationJobRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void create(UUID videoId) {
        jdbcTemplate.update(INSERT_JOB_SQL, videoId);
    }

    @Override
    @Transactional
    public Optional<ClaimedArchiveJob<VideoDurationCalculationJob>> claimNext(Duration stuckThreshold) {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime stuckThresholdTime = now.minus(stuckThreshold);

            VideoDurationCalculationJob job = jdbcTemplate.queryForObject(
                    SELECT_CLAIMABLE_JOB_SQL,
                    new VideoDurationCalculationJobRowMapper(),
                    Timestamp.valueOf(stuckThresholdTime)
            );

            if (job == null) {
                return Optional.empty();
            }

            JobStatus previousStatus = job.getStatus();
            LocalDateTime previousStartedAt = job.getStartedAt();

            jdbcTemplate.update(UPDATE_CLAIMED_STATUS_SQL, Timestamp.valueOf(now), job.getId());

            JobStatus claimedStatus = (previousStatus == JobStatus.PENDING) ? JobStatus.PROCESSING : JobStatus.RETRYING;
            job.setStatus(claimedStatus);
            job.setStartedAt(now);

            return Optional.of(new ClaimedArchiveJob<>(job, previousStatus, previousStartedAt));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean markCompleted(Long jobId) {
        return jdbcTemplate.update(UPDATE_TO_COMPLETED_SQL, jobId) == 1;
    }

    @Override
    public boolean markFailed(Long jobId) {
        return jdbcTemplate.update(UPDATE_TO_FAILED_SQL, jobId) == 1;
    }

    @Override
    public void resetToPreClaimState(Long jobId, JobStatus previousStatus, LocalDateTime previousStartedAt) {
        Timestamp startedAt = previousStartedAt == null ? null : Timestamp.valueOf(previousStartedAt);
        jdbcTemplate.update(con -> {
            var ps = con.prepareStatement(RESET_TO_PRE_CLAIM_SQL);
            ps.setString(1, previousStatus.name());
            if (startedAt == null) {
                ps.setNull(2, Types.TIMESTAMP);
            } else {
                ps.setTimestamp(2, startedAt);
            }
            ps.setLong(3, jobId);
            return ps;
        });
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
