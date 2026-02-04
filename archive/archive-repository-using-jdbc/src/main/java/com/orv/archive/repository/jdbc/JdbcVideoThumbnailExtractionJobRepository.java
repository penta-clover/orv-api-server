package com.orv.archive.repository.jdbc;

import com.orv.archive.domain.JobStatus;
import com.orv.archive.domain.VideoThumbnailExtractionJob;
import com.orv.archive.repository.VideoThumbnailExtractionJobRepository;
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
public class JdbcVideoThumbnailExtractionJobRepository implements VideoThumbnailExtractionJobRepository {

    private static final String SELECT_CLAIMABLE_JOB_SQL = """
            SELECT id, video_id, status, created_at, started_at
            FROM video_thumbnail_extraction_job
            WHERE status = 'PENDING'
               OR (status = 'PROCESSING' AND started_at < ?)
            ORDER BY id
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """;

    private static final String UPDATE_TO_PROCESSING_SQL = """
            UPDATE video_thumbnail_extraction_job
            SET status = 'PROCESSING', started_at = ?
            WHERE id = ?
            """;

    private static final String INSERT_JOB_SQL =
            "INSERT INTO video_thumbnail_extraction_job (video_id, status) VALUES (?, 'PENDING')";

    private static final String UPDATE_TO_COMPLETED_SQL =
            "UPDATE video_thumbnail_extraction_job SET status = 'COMPLETED' WHERE id = ?";

    private static final String UPDATE_TO_FAILED_SQL =
            "UPDATE video_thumbnail_extraction_job SET status = 'FAILED' WHERE id = ?";

    private final JdbcTemplate jdbcTemplate;

    public JdbcVideoThumbnailExtractionJobRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void create(UUID videoId) {
        jdbcTemplate.update(INSERT_JOB_SQL, videoId);
    }

    @Override
    @Transactional
    public Optional<VideoThumbnailExtractionJob> claimNext(Duration stuckThreshold) {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime stuckThresholdTime = now.minus(stuckThreshold);

            VideoThumbnailExtractionJob job = jdbcTemplate.queryForObject(
                    SELECT_CLAIMABLE_JOB_SQL,
                    new VideoThumbnailExtractionJobRowMapper(),
                    Timestamp.valueOf(stuckThresholdTime)
            );

            if (job == null) {
                return Optional.empty();
            }

            jdbcTemplate.update(UPDATE_TO_PROCESSING_SQL, Timestamp.valueOf(now), job.getId());

            job.setStatus(JobStatus.PROCESSING);
            job.setStartedAt(now);

            return Optional.of(job);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public void markCompleted(Long jobId) {
        jdbcTemplate.update(UPDATE_TO_COMPLETED_SQL, jobId);
    }

    @Override
    public void markFailed(Long jobId) {
        jdbcTemplate.update(UPDATE_TO_FAILED_SQL, jobId);
    }

    private static class VideoThumbnailExtractionJobRowMapper implements RowMapper<VideoThumbnailExtractionJob> {
        @Override
        public VideoThumbnailExtractionJob mapRow(ResultSet rs, int rowNum) throws SQLException {
            VideoThumbnailExtractionJob job = new VideoThumbnailExtractionJob();
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
