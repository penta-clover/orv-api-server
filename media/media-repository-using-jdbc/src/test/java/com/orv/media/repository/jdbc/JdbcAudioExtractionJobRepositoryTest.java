package com.orv.media.repository.jdbc;

import com.orv.media.domain.AudioExtractionJobStatus;
import com.orv.media.domain.ClaimedAudioJob;
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
class JdbcAudioExtractionJobRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    private static JdbcTemplate jdbcTemplate;
    private static JdbcAudioExtractionJobRepository repository;

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
                CREATE TABLE IF NOT EXISTS audio_extraction_job (
                    id BIGSERIAL PRIMARY KEY,
                    video_id UUID NOT NULL,
                    recap_reservation_id UUID,
                    member_id UUID NOT NULL,
                    storyboard_id UUID NOT NULL,
                    result_audio_recording_id UUID,
                    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    started_at TIMESTAMP
                )
                """);

        repository = new JdbcAudioExtractionJobRepository(jdbcTemplate) {
            @Override
            public Optional<ClaimedAudioJob> claimNext(Duration stuckThreshold) {
                return txTemplate.execute(status -> super.claimNext(stuckThreshold));
            }
        };
    }

    @BeforeEach
    void cleanTable() {
        jdbcTemplate.execute("TRUNCATE audio_extraction_job RESTART IDENTITY");
    }

    @Test
    void claimNext_promotesPendingToProcessing() {
        repository.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        Optional<ClaimedAudioJob> claimed = repository.claimNext(Duration.ofMinutes(15));

        assertThat(claimed).isPresent();
        assertThat(claimed.get().previousStatus()).isEqualTo(AudioExtractionJobStatus.PENDING);
        assertThat(claimed.get().job().getStatus()).isEqualTo(AudioExtractionJobStatus.PROCESSING);
    }

    @Test
    void claimNext_promotesStuckProcessingToRetrying() {
        LocalDateTime startedAt = LocalDateTime.now().minusMinutes(20).withNano(0);
        Long id = insertJob(AudioExtractionJobStatus.PROCESSING, startedAt);

        Optional<ClaimedAudioJob> claimed = repository.claimNext(Duration.ofMinutes(15));

        assertThat(claimed).isPresent();
        assertThat(claimed.get().previousStatus()).isEqualTo(AudioExtractionJobStatus.PROCESSING);
        assertThat(claimed.get().previousStartedAt()).isEqualTo(startedAt);
        assertThat(claimed.get().job().getStatus()).isEqualTo(AudioExtractionJobStatus.RETRYING);
        assertThat(currentStatus(id)).isEqualTo("RETRYING");
    }

    @Test
    void claimNext_skipsFreshProcessing() {
        insertJob(AudioExtractionJobStatus.PROCESSING, LocalDateTime.now().minusMinutes(5));

        assertThat(repository.claimNext(Duration.ofMinutes(15))).isEmpty();
    }

    @Test
    void claimNext_doesNotReclaimRetryingJob() {
        insertJob(AudioExtractionJobStatus.RETRYING, LocalDateTime.now().minusMinutes(60));

        assertThat(repository.claimNext(Duration.ofMinutes(15))).isEmpty();
    }

    @Test
    void markCompleted_returnsTrue_andStoresResultId_forProcessing() {
        Long id = insertJob(AudioExtractionJobStatus.PROCESSING, LocalDateTime.now());
        UUID resultId = UUID.randomUUID();

        assertThat(repository.markCompleted(id, resultId)).isTrue();
        assertThat(currentStatus(id)).isEqualTo("COMPLETED");
        UUID stored = jdbcTemplate.queryForObject(
                "SELECT result_audio_recording_id FROM audio_extraction_job WHERE id = ?", UUID.class, id);
        assertThat(stored).isEqualTo(resultId);
    }

    @Test
    void markCompleted_returnsTrue_forRetrying() {
        Long id = insertJob(AudioExtractionJobStatus.RETRYING, LocalDateTime.now());

        assertThat(repository.markCompleted(id, UUID.randomUUID())).isTrue();
        assertThat(currentStatus(id)).isEqualTo("COMPLETED");
    }

    @Test
    void markCompleted_returnsFalse_whenAlreadyTerminal() {
        Long completedId = insertJob(AudioExtractionJobStatus.COMPLETED, LocalDateTime.now());
        Long failedId = insertJob(AudioExtractionJobStatus.FAILED, LocalDateTime.now());

        assertThat(repository.markCompleted(completedId, UUID.randomUUID())).isFalse();
        assertThat(repository.markCompleted(failedId, UUID.randomUUID())).isFalse();
    }

    @Test
    void markFailed_returnsFalse_whenAlreadyCompleted() {
        Long id = insertJob(AudioExtractionJobStatus.COMPLETED, LocalDateTime.now());

        assertThat(repository.markFailed(id)).isFalse();
        assertThat(currentStatus(id)).isEqualTo("COMPLETED");
    }

    @Test
    void resetToPreClaimState_restoresPreviousStatusAndStartedAt() {
        LocalDateTime previousStartedAt = LocalDateTime.now().minusMinutes(30).withNano(0);
        Long id = insertJob(AudioExtractionJobStatus.RETRYING, LocalDateTime.now());

        repository.resetToPreClaimState(id, AudioExtractionJobStatus.PROCESSING, previousStartedAt);

        assertThat(currentStatus(id)).isEqualTo("PROCESSING");
        Timestamp ts = jdbcTemplate.queryForObject(
                "SELECT started_at FROM audio_extraction_job WHERE id = ?", Timestamp.class, id);
        assertThat(ts.toLocalDateTime()).isEqualTo(previousStartedAt);
    }

    @Test
    void resetToPreClaimState_doesNothing_whenAlreadyTerminal() {
        Long id = insertJob(AudioExtractionJobStatus.COMPLETED, LocalDateTime.now());

        repository.resetToPreClaimState(id, AudioExtractionJobStatus.PENDING, null);

        assertThat(currentStatus(id)).isEqualTo("COMPLETED");
    }

    private Long insertJob(AudioExtractionJobStatus status, LocalDateTime startedAt) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO audio_extraction_job (video_id, member_id, storyboard_id, status, started_at) VALUES (?, ?, ?, ?, ?) RETURNING id",
                Long.class,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                status.name(),
                startedAt == null ? null : Timestamp.valueOf(startedAt)
        );
    }

    private String currentStatus(Long id) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM audio_extraction_job WHERE id = ?", String.class, id);
    }
}
