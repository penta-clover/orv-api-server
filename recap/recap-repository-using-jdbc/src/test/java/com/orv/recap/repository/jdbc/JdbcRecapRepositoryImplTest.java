package com.orv.recap.repository.jdbc;

import com.orv.media.domain.InterviewAudioRecording;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JdbcRecapRepositoryImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("리캡 예약 ID로 연결된 오디오 정보를 조회한다")
    @SuppressWarnings("unchecked")
    void findAudioByRecapReservationId_success() {
        // Given
        UUID recapReservationId = UUID.randomUUID();
        UUID audioRecordingId = UUID.randomUUID();
        UUID storyboardId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        OffsetDateTime createdAt = OffsetDateTime.now();

        InterviewAudioRecording expectedRecording = InterviewAudioRecording.builder()
                .id(audioRecordingId)
                .storyboardId(storyboardId)
                .memberId(memberId)
                .audioUrl("https://s3.amazonaws.com/test-audio.opus")
                .createdAt(createdAt)
                .runningTime(324)
                .build();

        // The implementation uses jdbcTemplate.query(sql, ResultSetExtractor, args)
        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq(recapReservationId)))
                .thenReturn(Optional.of(expectedRecording));

        JdbcRecapReservationRepository repository = new JdbcRecapReservationRepository(jdbcTemplate);

        // When
        Optional<InterviewAudioRecording> result = repository.findAudioByRecapReservationId(recapReservationId);

        // Then
        assertThat(result).isPresent();
        InterviewAudioRecording audioRecording = result.get();
        assertThat(audioRecording.getId()).isEqualTo(audioRecordingId);
        assertThat(audioRecording.getStoryboardId()).isEqualTo(storyboardId);
        assertThat(audioRecording.getMemberId()).isEqualTo(memberId);
        assertThat(audioRecording.getAudioUrl()).isEqualTo("https://s3.amazonaws.com/test-audio.opus");
        assertThat(audioRecording.getRunningTime()).isEqualTo(324);
        assertThat(audioRecording.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("리캡 예약 ID로 연결된 오디오가 없으면 Optional.empty()를 반환한다")
    @SuppressWarnings("unchecked")
    void findAudioByRecapReservationId_notFound() {
        // Given
        UUID recapReservationId = UUID.randomUUID();

        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq(recapReservationId)))
                .thenReturn(Optional.empty());

        JdbcRecapReservationRepository repository = new JdbcRecapReservationRepository(jdbcTemplate);

        // When
        Optional<InterviewAudioRecording> result = repository.findAudioByRecapReservationId(recapReservationId);

        // Then
        assertThat(result).isEmpty();
    }
}
