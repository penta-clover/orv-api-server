package com.orv.api.unit.domain.reservation;

import com.orv.api.domain.media.service.dto.InterviewAudioRecording;
import com.orv.api.domain.reservation.repository.RecapRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class JdbcRecapRepositoryImplTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RecapRepository recapRepository;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE interview_audio_recording CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE recap_reservation CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE video CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE storyboard CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE member CASCADE");
    }

    private void insertTestData(UUID recapReservationId, UUID audioRecordingId, UUID videoId, 
                              UUID memberId, UUID storyboardId) {
        // Insert into member
        jdbcTemplate.update(
                "INSERT INTO member (id, nickname, provider, social_id, email, profile_image_url, phone_number, birthday, gender, name) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                memberId, "testUser", "testProvider", "social123", "test@example.com",
                "http://example.com/profile.jpg", "01012345678", LocalDate.of(2000, 1, 1), "male",
                "Test User");

        // Insert into storyboard
        jdbcTemplate.update("INSERT INTO storyboard (id, title, start_scene_id) VALUES (?, ?, ?)",
                storyboardId, "Test Storyboard", null);

        // Insert into video
        jdbcTemplate.update(
                "INSERT INTO video (id, storyboard_id, member_id, video_url, title, running_time, thumbnail_url, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, NOW())",
                videoId, storyboardId, memberId, "https://youtube.com", "Test Video", 324,
                "http://example.com/thumbnail.jpg");

        // Insert into interview_audio_recording
        jdbcTemplate.update(
                "INSERT INTO interview_audio_recording (id, storyboard_id, member_id, audio_url, created_at, running_time) VALUES (?, ?, ?, ?, ?, ?)",
                audioRecordingId, storyboardId, memberId, "https://s3.amazonaws.com/test-audio.opus", 
                OffsetDateTime.now(), 324);

        // Insert into recap_reservation with linked audio recording
        jdbcTemplate.update(
                "INSERT INTO recap_reservation (id, member_id, video_id, scheduled_at, interview_audio_recording_id) VALUES (?, ?, ?, ?, ?)",
                recapReservationId, memberId, videoId, OffsetDateTime.now(), audioRecordingId);
    }

    @Test
    @DisplayName("리캡 예약 ID로 연결된 오디오 정보를 조회한다")
    void findAudioByRecapReservationId_success() {
        // Given
        UUID recapReservationId = UUID.randomUUID();
        UUID audioRecordingId = UUID.randomUUID();
        UUID videoId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        UUID storyboardId = UUID.randomUUID();

        insertTestData(recapReservationId, audioRecordingId, videoId, memberId, storyboardId);

        // When
        Optional<InterviewAudioRecording> result = recapRepository.findAudioByRecapReservationId(recapReservationId);

        // Then
        assertThat(result).isPresent();
        InterviewAudioRecording audioRecording = result.get();
        assertThat(audioRecording.getId()).isEqualTo(audioRecordingId);
        assertThat(audioRecording.getStoryboardId()).isEqualTo(storyboardId);
        assertThat(audioRecording.getMemberId()).isEqualTo(memberId);
        assertThat(audioRecording.getAudioUrl()).isEqualTo("https://s3.amazonaws.com/test-audio.opus");
        assertThat(audioRecording.getRunningTime()).isEqualTo(324);
        assertThat(audioRecording.getCreatedAt()).isNotNull();
    }
}
