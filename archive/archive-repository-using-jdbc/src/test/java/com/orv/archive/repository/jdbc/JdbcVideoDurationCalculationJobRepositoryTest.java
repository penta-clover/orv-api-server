package com.orv.archive.repository.jdbc;

import com.orv.archive.domain.ClaimedArchiveJob;
import com.orv.archive.domain.JobStatus;
import com.orv.archive.domain.VideoDurationCalculationJob;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class JdbcVideoDurationCalculationJobRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    private static JdbcTemplate jdbcTemplate;
    private static JdbcVideoDurationCalculationJobRepository repository;

    @BeforeAll
    static void setUp() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(postgres.getJdbcUrl());
        config.setUsername(postgres.getUsername());
        config.setPassword(postgres.getPassword());
        DataSource dataSource = new HikariDataSource(config);
        jdbcTemplate = new JdbcTemplate(dataSource);

        DataSourceTransactionManager txManager = new DataSourceTransactionManager(dataSource);
        TransactionTemplate txTemplate = new TransactionTemplate(txManager);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS video_duration_extraction_job (
                    id BIGSERIAL PRIMARY KEY,
                    video_id UUID NOT NULL,
                    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    started_at TIMESTAMP
                )
                """);

        repository = new JdbcVideoDurationCalculationJobRepository(jdbcTemplate) {
            @Override
            public Optional<ClaimedArchiveJob<VideoDurationCalculationJob>> claimNext(Duration stuckThreshold) {
                return txTemplate.execute(status -> super.claimNext(stuckThreshold));
            }
        };
    }

    @BeforeEach
    void cleanTable() {
        jdbcTemplate.execute("TRUNCATE video_duration_extraction_job RESTART IDENTITY");
    }

    @Test
    void claimNext_returnsEmpty_whenNoClaimableJob() {
        Optional<ClaimedArchiveJob<VideoDurationCalculationJob>> claimed = repository.claimNext(Duration.ofMinutes(10));
        assertThat(claimed).isEmpty();
    }

    @Test
    void claimNext_promotesPendingToProcessing() {
        repository.create(UUID.randomUUID());

        Optional<ClaimedArchiveJob<VideoDurationCalculationJob>> claimed = repository.claimNext(Duration.ofMinutes(10));

        assertThat(claimed).isPresent();
        assertThat(claimed.get().previousStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(claimed.get().previousStartedAt()).isNull();
        assertThat(claimed.get().job().getStatus()).isEqualTo(JobStatus.PROCESSING);
        assertThat(claimed.get().job().getStartedAt()).isNotNull();
    }

    @Test
    void claimNext_skipsFreshProcessingJob() {
        Long id = insertJob(JobStatus.PROCESSING, LocalDateTime.now().minusMinutes(5));

        Optional<ClaimedArchiveJob<VideoDurationCalculationJob>> claimed = repository.claimNext(Duration.ofMinutes(10));

        assertThat(claimed).isEmpty();
        assertThat(currentStatus(id)).isEqualTo("PROCESSING");
    }

    @Test
    void claimNext_promotesStuckProcessingToRetrying() {
        LocalDateTime originalStartedAt = LocalDateTime.now().minusMinutes(15).withNano(0);
        Long id = insertJob(JobStatus.PROCESSING, originalStartedAt);

        Optional<ClaimedArchiveJob<VideoDurationCalculationJob>> claimed = repository.claimNext(Duration.ofMinutes(10));

        assertThat(claimed).isPresent();
        assertThat(claimed.get().previousStatus()).isEqualTo(JobStatus.PROCESSING);
        assertThat(claimed.get().previousStartedAt()).isEqualTo(originalStartedAt);
        assertThat(claimed.get().job().getId()).isEqualTo(id);
        assertThat(claimed.get().job().getStatus()).isEqualTo(JobStatus.RETRYING);
        assertThat(currentStatus(id)).isEqualTo("RETRYING");
    }

    @Test
    void claimNext_doesNotReclaimRetryingJob() {
        insertJob(JobStatus.RETRYING, LocalDateTime.now().minusMinutes(60));

        Optional<ClaimedArchiveJob<VideoDurationCalculationJob>> claimed = repository.claimNext(Duration.ofMinutes(10));

        assertThat(claimed).isEmpty();
    }

    @Test
    void markCompleted_returnsTrue_whenStatusIsProcessing() {
        Long id = insertJob(JobStatus.PROCESSING, LocalDateTime.now());

        assertThat(repository.markCompleted(id)).isTrue();
        assertThat(currentStatus(id)).isEqualTo("COMPLETED");
    }

    @Test
    void markCompleted_returnsTrue_whenStatusIsRetrying() {
        Long id = insertJob(JobStatus.RETRYING, LocalDateTime.now());

        assertThat(repository.markCompleted(id)).isTrue();
        assertThat(currentStatus(id)).isEqualTo("COMPLETED");
    }

    @Test
    void markCompleted_returnsFalse_whenAlreadyCompleted() {
        Long id = insertJob(JobStatus.COMPLETED, LocalDateTime.now());

        assertThat(repository.markCompleted(id)).isFalse();
        assertThat(currentStatus(id)).isEqualTo("COMPLETED");
    }

    @Test
    void markCompleted_returnsFalse_whenAlreadyFailed() {
        Long id = insertJob(JobStatus.FAILED, LocalDateTime.now());

        assertThat(repository.markCompleted(id)).isFalse();
        assertThat(currentStatus(id)).isEqualTo("FAILED");
    }

    @Test
    void markFailed_returnsFalse_whenAlreadyCompleted() {
        Long id = insertJob(JobStatus.COMPLETED, LocalDateTime.now());

        assertThat(repository.markFailed(id)).isFalse();
        assertThat(currentStatus(id)).isEqualTo("COMPLETED");
    }

    @Test
    void resetToPreClaimState_restoresPendingFromProcessing() {
        Long id = insertJob(JobStatus.PROCESSING, LocalDateTime.now());

        repository.resetToPreClaimState(id, JobStatus.PENDING, null);

        assertThat(currentStatus(id)).isEqualTo("PENDING");
        Timestamp ts = jdbcTemplate.queryForObject(
                "SELECT started_at FROM video_duration_extraction_job WHERE id = ?", Timestamp.class, id);
        assertThat(ts).isNull();
    }

    @Test
    void resetToPreClaimState_restoresProcessingFromRetrying() {
        LocalDateTime originalStartedAt = LocalDateTime.now().minusMinutes(20).withNano(0);
        Long id = insertJob(JobStatus.RETRYING, LocalDateTime.now());

        repository.resetToPreClaimState(id, JobStatus.PROCESSING, originalStartedAt);

        assertThat(currentStatus(id)).isEqualTo("PROCESSING");
        Timestamp ts = jdbcTemplate.queryForObject(
                "SELECT started_at FROM video_duration_extraction_job WHERE id = ?", Timestamp.class, id);
        assertThat(ts.toLocalDateTime()).isEqualTo(originalStartedAt);
    }

    @Test
    void resetToPreClaimState_doesNothing_whenAlreadyCompleted() {
        Long id = insertJob(JobStatus.COMPLETED, LocalDateTime.now());

        repository.resetToPreClaimState(id, JobStatus.PENDING, null);

        assertThat(currentStatus(id)).isEqualTo("COMPLETED");
    }

    private Long insertJob(JobStatus status, LocalDateTime startedAt) {
        UUID videoId = UUID.randomUUID();
        return jdbcTemplate.queryForObject(
                "INSERT INTO video_duration_extraction_job (video_id, status, started_at) VALUES (?, ?, ?) RETURNING id",
                Long.class,
                videoId,
                status.name(),
                startedAt == null ? null : Timestamp.valueOf(startedAt)
        );
    }

    private String currentStatus(Long id) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM video_duration_extraction_job WHERE id = ?", String.class, id);
    }
}
