package com.orv.archive.repository.jdbc;

import com.orv.archive.domain.ThumbnailCandidate;
import com.orv.archive.repository.ThumbnailCandidateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Slf4j
public class JdbcThumbnailCandidateRepository implements ThumbnailCandidateRepository {

    private static final String INSERT_SQL = """
            INSERT INTO video_thumbnail_candidate (job_id, video_id, timestamp_ms, file_key)
            VALUES (?, ?, ?, ?)
            RETURNING id
            """;

    private static final String SELECT_BY_JOB_ID_SQL = """
            SELECT id, job_id, video_id, timestamp_ms, file_key, created_at
            FROM video_thumbnail_candidate
            WHERE job_id = ?
            ORDER BY id
            """;

    private static final String SELECT_BY_VIDEO_ID_SQL = """
            SELECT id, job_id, video_id, timestamp_ms, file_key, created_at
            FROM video_thumbnail_candidate
            WHERE video_id = ?
            ORDER BY id
            """;

    private static final String SELECT_BY_ID_SQL = """
            SELECT id, job_id, video_id, timestamp_ms, file_key, created_at
            FROM video_thumbnail_candidate
            WHERE id = ?
            """;

    private static final String DELETE_BY_JOB_ID_SQL = """
            DELETE FROM video_thumbnail_candidate
            WHERE job_id = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcThumbnailCandidateRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Long save(ThumbnailCandidate candidate) {
        return jdbcTemplate.queryForObject(INSERT_SQL, Long.class,
                candidate.getJobId(),
                candidate.getVideoId(),
                candidate.getTimestampMs(),
                candidate.getFileKey()
        );
    }

    @Override
    public Optional<ThumbnailCandidate> findById(Long id) {
        List<ThumbnailCandidate> results = jdbcTemplate.query(SELECT_BY_ID_SQL, new ThumbnailCandidateRowMapper(), id);
        return results.stream().findFirst();
    }

    @Override
    public List<ThumbnailCandidate> findByJobId(Long jobId) {
        return jdbcTemplate.query(SELECT_BY_JOB_ID_SQL, new ThumbnailCandidateRowMapper(), jobId);
    }

    @Override
    public List<ThumbnailCandidate> findByVideoId(UUID videoId) {
        return jdbcTemplate.query(SELECT_BY_VIDEO_ID_SQL, new ThumbnailCandidateRowMapper(), videoId);
    }

    @Override
    public void deleteByJobId(Long jobId) {
        jdbcTemplate.update(DELETE_BY_JOB_ID_SQL, jobId);
    }
}
