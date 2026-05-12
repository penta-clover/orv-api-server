package com.orv.media.repository.jdbc;

import com.orv.media.domain.AudioExtractionJob;
import com.orv.media.domain.AudioExtractionJobStatus;
import com.orv.media.domain.ClaimedAudioJob;
import com.orv.media.repository.AudioExtractionJobRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
@Slf4j
public class JdbcAudioExtractionJobRepository implements AudioExtractionJobRepository {

    private static final String INSERT_JOB_SQL = """
            INSERT INTO audio_extraction_job (video_id, recap_reservation_id, member_id, storyboard_id, status)
            VALUES (?, ?, ?, ?, 'PENDING')
            """;

    private static final String SELECT_CLAIMABLE_JOB_SQL = """
            SELECT id, video_id, recap_reservation_id, member_id, storyboard_id, result_audio_recording_id, status, created_at, started_at
            FROM audio_extraction_job
            WHERE status = 'PENDING'
               OR (status = 'PROCESSING' AND started_at < ?)
            ORDER BY id
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """;

    private static final String UPDATE_CLAIMED_STATUS_SQL = """
            UPDATE audio_extraction_job
            SET status = CASE status
                            WHEN 'PENDING' THEN 'PROCESSING'
                            WHEN 'PROCESSING' THEN 'RETRYING'
                         END,
                started_at = ?
            WHERE id = ?
            """;

    private static final String UPDATE_TO_COMPLETED_SQL =
            "UPDATE audio_extraction_job SET status = 'COMPLETED', result_audio_recording_id = ? WHERE id = ? AND status IN ('PROCESSING', 'RETRYING')";

    private static final String UPDATE_TO_FAILED_SQL =
            "UPDATE audio_extraction_job SET status = 'FAILED' WHERE id = ? AND status IN ('PROCESSING', 'RETRYING')";

    private static final String RESET_TO_PRE_CLAIM_SQL =
            "UPDATE audio_extraction_job SET status = ?, started_at = ? WHERE id = ? AND status IN ('PROCESSING', 'RETRYING')";

    private final JdbcTemplate jdbcTemplate;

    public JdbcAudioExtractionJobRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void create(UUID videoId, UUID recapReservationId, UUID memberId, UUID storyboardId) {
        jdbcTemplate.update(INSERT_JOB_SQL, videoId, recapReservationId, memberId, storyboardId);
    }

    @Override
    @Transactional
    public Optional<ClaimedAudioJob> claimNext(Duration stuckThreshold) {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime stuckThresholdTime = now.minus(stuckThreshold);

            AudioExtractionJob job = jdbcTemplate.queryForObject(
                    SELECT_CLAIMABLE_JOB_SQL,
                    new AudioExtractionJobRowMapper(),
                    Timestamp.valueOf(stuckThresholdTime)
            );

            if (job == null) {
                return Optional.empty();
            }

            AudioExtractionJobStatus previousStatus = job.getStatus();
            LocalDateTime previousStartedAt = job.getStartedAt();

            jdbcTemplate.update(UPDATE_CLAIMED_STATUS_SQL, Timestamp.valueOf(now), job.getId());

            AudioExtractionJobStatus claimedStatus =
                    (previousStatus == AudioExtractionJobStatus.PENDING)
                            ? AudioExtractionJobStatus.PROCESSING
                            : AudioExtractionJobStatus.RETRYING;
            job.setStatus(claimedStatus);
            job.setStartedAt(now);

            return Optional.of(new ClaimedAudioJob(job, previousStatus, previousStartedAt));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean markCompleted(Long jobId, UUID resultAudioRecordingId) {
        return jdbcTemplate.update(UPDATE_TO_COMPLETED_SQL, resultAudioRecordingId, jobId) == 1;
    }

    @Override
    public boolean markFailed(Long jobId) {
        return jdbcTemplate.update(UPDATE_TO_FAILED_SQL, jobId) == 1;
    }

    @Override
    public void resetToPreClaimState(Long jobId, AudioExtractionJobStatus previousStatus, LocalDateTime previousStartedAt) {
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
}
