package com.orv.api.unit.domain.recap;

import com.orv.api.domain.recap.repository.RecapResultRepository;
import com.orv.api.domain.recap.controller.dto.RecapAnswerSummaryResponse;
import com.orv.api.domain.recap.controller.dto.RecapResultResponse;

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
class RecapResultRepositoryTest {

        @Autowired
        private JdbcTemplate jdbcTemplate;

        @Autowired
        private RecapResultRepository recapResultRepository;

        @BeforeEach
        void cleanDatabase() {
                jdbcTemplate.execute("TRUNCATE TABLE recap_answer_summary CASCADE");
                jdbcTemplate.execute("TRUNCATE TABLE recap_result CASCADE");
                jdbcTemplate.execute("TRUNCATE TABLE recap_reservation CASCADE");
                // scene 테이블은 다른 테스트나 실제 데이터에 의존할 수 있으므로 TRUNCATE하지 않습니다.
                // 대신 테스트 데이터 삽입 시 필요한 scene만 삽입하거나, 기존 scene을 활용합니다.
        }

        private void insertTestData(UUID recapReservationId, UUID recapResultId, UUID videoId, UUID memberId,
                        UUID storyboardId, UUID sceneId1, UUID sceneId2) {
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

                // Insert into recap_result
                jdbcTemplate.update("INSERT INTO recap_result (id, created_at) VALUES (?, ?)",
                                recapResultId, OffsetDateTime.now());

                // Insert into recap_reservation
                jdbcTemplate.update(
                                "INSERT INTO recap_reservation (id, member_id, video_id, scheduled_at, recap_result_id) VALUES (?, ?, ?, ?, ?)",
                                recapReservationId, memberId, videoId, OffsetDateTime.now(), recapResultId);

                // Insert into scene (using provided example data for content)
                // Note: In a real scenario, these scenes might already exist or be created as
                // part of storyboard setup.
                // For this test, we ensure they exist.
                jdbcTemplate.update(
                                "INSERT INTO scene (id, storyboard_id, name, scene_type, content) VALUES (?, ?, ?, ?, CAST(? AS jsonb)) ON CONFLICT (id) DO NOTHING",
                                sceneId1, storyboardId, "scene_title", "QUESTION",
                                "{\"question\" : \"가벼운 인사 한마디 부탁 드립니다.\", \"hint\" : \"시간이 지난 후에 이 영상을 다시 보았을 때, '나는 이런 사람이었구나'라는 생각이 들 수 있게 표현해주세요. 나이, 이름, 나를 표현하는 말 등을 함께 말해주셔도 좋아요.\", \"nextSceneId\" : \"B7CA99D7-55D6-4EB1-9102-8957C1275EE5\", \"isHiddenQuestion\" : false}");
                jdbcTemplate.update(
                                "INSERT INTO scene (id, storyboard_id, name, scene_type, content) VALUES (?, ?, ?, ?, CAST(? AS jsonb)) ON CONFLICT (id) DO NOTHING",
                                sceneId2, storyboardId, "scene_title", "QUESTION",
                                "{\"question\" : \"@{name}님은 왜 HySpark에 들어 오려고 했나요?\", \"hint\" : \"들어오기 전 무엇을 기대했고 어떤 마음으로 지원했었나요?\", \"nextSceneId\" : \"C247A878-A3C0-4788-9AF4-212E60795253\", \"isHiddenQuestion\" : false}");

                // Insert into recap_answer_summary
                jdbcTemplate.update(
                                "INSERT INTO recap_answer_summary (recap_result_id, scene_id, summary, scene_order) VALUES (?, ?, ?, ?)",
                                recapResultId, sceneId1, "안녕하세요. 저는 홍길동입니다.", 0);
                jdbcTemplate.update(
                                "INSERT INTO recap_answer_summary (recap_result_id, scene_id, summary, scene_order) VALUES (?, ?, ?, ?)",
                                recapResultId, sceneId2, "HySpark에 대한 기대가 컸습니다.", 1);
        }

        @Test
        @DisplayName("리캡 예약 ID로 리캡 결과와 답변 요약을 조회한다")
        void findByRecapReservationId_success() {
                // Given
                UUID recapReservationId = UUID.randomUUID();
                UUID recapResultId = UUID.randomUUID();
                UUID videoId = UUID.randomUUID();
                UUID memberId = UUID.randomUUID();
                UUID storyboardId = UUID.randomUUID();
                UUID sceneId1 = UUID.fromString("b33dbf34-7f5d-47db-84f6-0c846eeb0b6a"); // Use provided UUID
                UUID sceneId2 = UUID.fromString("b7ca99d7-55d6-4eb1-9102-8957c1275ee5"); // Use provided UUID

                insertTestData(recapReservationId, recapResultId, videoId, memberId, storyboardId, sceneId1, sceneId2);

                // When
                Optional<RecapResultResponse> result = recapResultRepository
                                .findByRecapReservationId(recapReservationId);

                // Then
                assertThat(result).isPresent();
                RecapResultResponse response = result.get();
                assertThat(response.getRecapResultId()).isEqualTo(recapResultId);
                assertThat(response.getCreatedAt()).isNotNull();
                assertThat(response.getAnswerSummaries()).hasSize(2);

                RecapAnswerSummaryResponse summary1 = response.getAnswerSummaries().get(0);
                assertThat(summary1.getSceneId()).isEqualTo(sceneId1);
                assertThat(summary1.getQuestion()).isEqualTo("가벼운 인사 한마디 부탁 드립니다.");
                assertThat(summary1.getAnswerSummary()).isEqualTo("안녕하세요. 저는 홍길동입니다.");

                RecapAnswerSummaryResponse summary2 = response.getAnswerSummaries().get(1);
                assertThat(summary2.getSceneId()).isEqualTo(sceneId2);
                assertThat(summary2.getQuestion()).isEqualTo("@{name}님은 왜 HySpark에 들어 오려고 했나요?");
                assertThat(summary2.getAnswerSummary()).isEqualTo("HySpark에 대한 기대가 컸습니다.");
        }

        @Test
        @DisplayName("존재하지 않는 리캡 예약 ID로 조회 시 Optional.empty()를 반환한다")
        void findByRecapReservationId_notFound() {
                // Given
                UUID nonExistentRecapReservationId = UUID.randomUUID();

                // When
                Optional<RecapResultResponse> result = recapResultRepository
                                .findByRecapReservationId(nonExistentRecapReservationId);

                // Then
                assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("리캡 예약은 존재하지만 연결된 리캡 결과가 없는 경우 Optional.empty()를 반환한다")
        void findByRecapReservationId_noRecapResultLinked() {
                // Given
                UUID recapReservationId = UUID.randomUUID();
                UUID videoId = UUID.randomUUID();
                UUID memberId = UUID.randomUUID();
                UUID storyboardId = UUID.randomUUID();

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


                // Insert into recap_reservation without linking recap_result_id
                jdbcTemplate.update(
                                "INSERT INTO recap_reservation (id, member_id, video_id, scheduled_at, recap_result_id) VALUES (?, ?, ?, ?, NULL)",
                                recapReservationId, memberId, videoId, OffsetDateTime.now());

                // When
                Optional<RecapResultResponse> result = recapResultRepository
                                .findByRecapReservationId(recapReservationId);

                // Then
                assertThat(result).isEmpty();
        }
}