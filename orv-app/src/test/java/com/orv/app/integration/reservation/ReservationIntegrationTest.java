package com.orv.app.integration.reservation;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.amazonaws.services.s3.AmazonS3;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.orv.auth.service.JwtTokenService;
import com.orv.reservation.controller.dto.InterviewReservationRequest;
import com.orv.recap.controller.dto.RecapReservationRequest;
import com.orv.recap.external.RecapClient;
import com.orv.recap.domain.RecapContent;
import com.orv.recap.external.dto.RecapServerResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
public class ReservationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    // Scheduler 빈이 실제로 호출되는지 확인 (실제 동작 여부는 Quartz 테스트 전략에 따라 결정)
    @Autowired
    private Scheduler scheduler;

    @Autowired
    private JwtTokenService jwtTokenProvider;

    @MockitoBean
    private AmazonS3 amazonS3;

    @MockitoBean
    private RecapClient recapClient;

    private String testMemberId;

    private String testStoryboardId;

    private String testTopicId;

    private String testVideoId;

    private String testScene1Id;

    private String testScene2Id;

    private static String token;

    @BeforeEach
    public void setUp() {
        testMemberId = UUID.randomUUID().toString();
        testStoryboardId = UUID.randomUUID().toString();
        testTopicId = UUID.randomUUID().toString();
        testVideoId = UUID.randomUUID().toString();
        testScene1Id = UUID.randomUUID().toString();
        testScene2Id = UUID.randomUUID().toString();

        // 테스트 실행 전, SecurityContext에 테스트용 회원 ID 설정
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(testMemberId, null));

        token = jwtTokenProvider.createToken(testMemberId, Map.of("provider", "google", "socialId", "21342342523"));

        // 테스트를 위해 모든 관련 테이블 초기화 (CASCADE 옵션에 의존하지 않도록 순서를 고려)
        jdbcTemplate.update("DELETE FROM recap_reservation");
        jdbcTemplate.update("DELETE FROM interview_reservation");
        jdbcTemplate.update("DELETE FROM video");
        jdbcTemplate.update("DELETE FROM term_agreement");
        jdbcTemplate.update("DELETE FROM storyboard_usage_history");
        jdbcTemplate.update("DELETE FROM member_role");
        jdbcTemplate.update("DELETE FROM role");
        jdbcTemplate.update("DELETE FROM member");

        // member 테이블에 테스트 회원 데이터 삽입
        jdbcTemplate.update("INSERT INTO member (id, nickname, provider, social_id, email, profile_image_url, phone_number, birthday, gender, name) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                UUID.fromString(testMemberId), "testUser", "testProvider", "social123", "test@example.com", "http://example.com/profile.jpg", "01012345678", LocalDate.of(2000, 1, 1), "male", "Test User");

        // storyboard 테이블에 테스트 스토리보드 데이터 삽입
        jdbcTemplate.update("INSERT INTO storyboard (id, title, start_scene_id) VALUES (?, ?, ?)",
                UUID.fromString(testStoryboardId), "Test Storyboard", null);

        // topic 테이블에 테스트 주제 데이터 삽입
        jdbcTemplate.update("INSERT INTO topic (id, name, description, thumbnail_url) VALUES (?, ?, ?, ?)",
                UUID.fromString(testTopicId), "Test Topic", "Test Description", "http://example.com/thumbnail.jpg");

        // storyboard_topic 테이블에 테스트 스토리보드-주제 관계 데이터 삽입
        jdbcTemplate.update("INSERT INTO storyboard_topic (storyboard_id, topic_id) VALUES (?, ?)",
                UUID.fromString(testStoryboardId), UUID.fromString(testTopicId));

        // video 테이블에 테스트 비디오 데이터 삽입
        jdbcTemplate.update("INSERT INTO video (id, storyboard_id, member_id, video_url, title, running_time, thumbnail_url, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, NOW())",
                UUID.fromString(testVideoId), UUID.fromString(testStoryboardId), UUID.fromString(testMemberId), "https://d3bdjeyz3ry3pi.cloudfront.net/archive/videos/1fae8eed-3e88-4a9c-9e1e-96bed6414f9f", "Test Video", 324, "http://example.com/thumbnail.jpg");
    }

    /**
     * 인터뷰 예약 통합 테스트
     * - API 호출 시 올바른 예약 생성 및 DB에 데이터 저장
     */
    @Test
    public void testReserveInterview() throws Exception {
        // 준비: 요청 JSON 생성
        InterviewReservationRequest request = new InterviewReservationRequest();
        // storyboardId 및 reservedAt 설정
        request.setStoryboardId(testStoryboardId);
        request.setReservedAt(ZonedDateTime.now().plusDays(1));

        String requestJson = objectMapper.writeValueAsString(request);

        // API 호출: POST /api/v0/reservation/interview
        String responseContent = mockMvc.perform(post("/api/v0/reservation/interview")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andReturn().getResponse().getContentAsString();

        // 검증: interview_reservation 테이블에 데이터 삽입 확인
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM interview_reservation", Integer.class);
        assertThat(count).isGreaterThan(0);
    }

    /**
     * 예약된 인터뷰 조회 통합 테스트
     */
    @Test
    public void testGetForwardInterviews() throws Exception {
        // 테스트 데이터 삽입: 인터뷰 예약 추가
        UUID reservationId = UUID.randomUUID();
        // storyboard_id도 UUID 타입이라면 UUID 객체 전달 (혹은 toString() 후, SQL에서 캐스팅할 수 있음)
        UUID storyboardId = UUID.fromString(testStoryboardId);
        UUID memberId = UUID.fromString(testMemberId);
        jdbcTemplate.update(
                "INSERT INTO interview_reservation (id, member_id, storyboard_id, scheduled_at, created_at, reservation_status) VALUES (?, ?, ?, NOW(), NOW(), ?)",
                reservationId, memberId, storyboardId, "pending"
        );

        // API 호출: GET /api/v0/reservation/interview/forward
        mockMvc.perform(get("/api/v0/reservation/interview/forward")
                        .header("Authorization", "Bearer " + token)
                        .param("from", OffsetDateTime.now().toString()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.data").isArray());
    }

    /**
     * 인터뷰 완료 처리 통합 테스트
     */
    @Test
    public void testDoneInterview() throws Exception {
        // 테스트 데이터 삽입: pending 상태의 예약 생성
        UUID reservationId = UUID.randomUUID();
        UUID storyboardId = UUID.fromString(testStoryboardId);
        UUID memberId = UUID.fromString(testMemberId);

        jdbcTemplate.update(
                "INSERT INTO interview_reservation (id, member_id, storyboard_id, scheduled_at, created_at, reservation_status) VALUES (?, ?, ?, NOW(), NOW(), ?)",
                reservationId, memberId, storyboardId, "pending"
        );

        // API 호출: PATCH /api/v0/reservation/interview/{reservationId}/done
        mockMvc.perform(patch("/api/v0/reservation/interview/{interviewId}/done", reservationId.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.statusCode").value(200));

        // 검증: DB에서 예약 상태가 'done'으로 변경되었는지 확인
        String status = jdbcTemplate.queryForObject(
                "SELECT reservation_status FROM interview_reservation WHERE id = ?",
                new Object[]{reservationId},
                String.class
        );
        assertThat(status).isEqualTo("done");
    }

    /**
     * 리캡 예약 통합 테스트
     */
    @Test
    public void testReserveRecap() throws Exception {
        RecapServerResponse mockResponse = new RecapServerResponse(
                List.of(
                        new RecapContent(UUID.fromString(testScene1Id), "summary1"),
                        new RecapContent(UUID.fromString(testScene2Id), "summary2")
                )
        );
        when(recapClient.requestRecap(any())).thenReturn(Optional.of(mockResponse));

        // 준비: 요청 JSON 생성
        RecapReservationRequest request = new RecapReservationRequest();
        // video_id 컬럼이 uuid라면, UUID 객체를 toString() 대신 직접 전달하거나 SQL에서 캐스팅 적용
        request.setVideoId(testVideoId); // 만약 DB에 문자열로 저장된다면 이렇게, 아니면 UUID 객체로 직접 전달
        request.setScheduledAt(ZonedDateTime.now().plusDays(1));

        String requestJson = objectMapper.writeValueAsString(request);

        // API 호출: POST /api/v0/reservation/recap/video
        String responseContent = mockMvc.perform(post("/api/v0/reservation/recap/video")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andReturn().getResponse().getContentAsString();

        // 검증: recap_reservation 테이블에 데이터 삽입 확인
        Integer recapReservationCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM recap_reservation", Integer.class);
        Integer audioRecordingCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM interview_audio_recording", Integer.class);
        assertThat(recapReservationCount).isGreaterThan(0);
        assertThat(audioRecordingCount).isGreaterThan(0);
    }

    private void insertRecapResultTestData(UUID recapReservationId, UUID recapResultId, UUID memberId, UUID videoId, UUID storyboardId, UUID sceneId1, UUID sceneId2) {
        // Insert into recap_result
        jdbcTemplate.update("INSERT INTO recap_result (id, created_at) VALUES (?, ?)",
                recapResultId, OffsetDateTime.now());

        // Insert into recap_reservation, linking to recap_result
        jdbcTemplate.update("INSERT INTO recap_reservation (id, member_id, video_id, scheduled_at, recap_result_id) VALUES (?, ?, ?, ?, ?)",
                recapReservationId, memberId, videoId, OffsetDateTime.now(), recapResultId);

        // Insert into scene (ensure these exist for the join)
        // Using ON CONFLICT (id) DO NOTHING to avoid issues if scenes are pre-populated
        jdbcTemplate.update("INSERT INTO scene (id, storyboard_id, name, scene_type, content) VALUES (?, ?, ?, ?, CAST(? AS jsonb)) ON CONFLICT (id) DO NOTHING",
                sceneId1, storyboardId, "Scene 1 Title", "QUESTION", "{\"question\" : \"가벼운 인사 한마디 부탁 드립니다.\", \"hint\" : \"hint1\"}");
        jdbcTemplate.update("INSERT INTO scene (id, storyboard_id, name, scene_type, content) VALUES (?, ?, ?, ?, CAST(? AS jsonb)) ON CONFLICT (id) DO NOTHING",
                sceneId2, storyboardId, "Scene 2 Title", "QUESTION", "{\"question\" : \" @{name}님은 왜 HySpark에 들어 오려고 했나요?\", \"hint\" : \"hint2\"}");

        // Insert into recap_answer_summary
        jdbcTemplate.update("INSERT INTO recap_answer_summary (recap_result_id, scene_id, summary, scene_order) VALUES (?, ?, ?, ?)",
                recapResultId, sceneId1, "안녕하세요. 저는 홍길동입니다.", 0);
        jdbcTemplate.update("INSERT INTO recap_answer_summary (recap_result_id, scene_id, summary, scene_order) VALUES (?, ?, ?, ?)",
                recapResultId, sceneId2, "HySpark에 대한 기대가 컸습니다.", 1);
    }

    /**
     * 리캡 결과 조회 통합 테스트
     */
    @Test
    public void testGetRecapResult_success() throws Exception {
        // Given
        UUID recapReservationId = UUID.randomUUID();
        UUID recapResultId = UUID.randomUUID();
        UUID sceneId1 = UUID.fromString("b33dbf34-7f5d-47db-84f6-0c846eeb0b6a");
        UUID sceneId2 = UUID.fromString("b7ca99d7-55d6-4eb1-9102-8957c1275ee5");

        insertRecapResultTestData(recapReservationId, recapResultId, UUID.fromString(testMemberId), UUID.fromString(testVideoId), UUID.fromString(testStoryboardId), sceneId1, sceneId2);

        // When & Then
        mockMvc.perform(get("/api/v0/reservation/recap/{recapReservationId}/result", recapReservationId.toString())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.recapResultId").value(recapResultId.toString()))
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.data.answerSummaries").isArray())
                .andExpect(jsonPath("$.data.answerSummaries[0].sceneId").value(sceneId1.toString()))
                .andExpect(jsonPath("$.data.answerSummaries[0].question").value("가벼운 인사 한마디 부탁 드립니다."))
                .andExpect(jsonPath("$.data.answerSummaries[0].answerSummary").value("안녕하세요. 저는 홍길동입니다."))
                .andExpect(jsonPath("$.data.answerSummaries[1].sceneId").value(sceneId2.toString()))
                .andExpect(jsonPath("$.data.answerSummaries[1].question").value("@{name}님은 왜 HySpark에 들어 오려고 했나요?"))
                .andExpect(jsonPath("$.data.answerSummaries[1].answerSummary").value("HySpark에 대한 기대가 컸습니다."));
    }

    @Test
    public void testGetRecapResult_notFound() throws Exception {
        // Given
        UUID nonExistentRecapReservationId = UUID.randomUUID();

        // When & Then
        mockMvc.perform(get("/api/v0/reservation/recap/{nonExistentRecapReservationId}/result", nonExistentRecapReservationId.toString())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.statusCode").value(404));
    }
}
