// ReservationIntegrationTest.java
package com.orv.api.integration;

import com.amazonaws.services.s3.AmazonS3;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orv.api.domain.auth.JwtTokenProvider;
import com.orv.api.domain.reservation.dto.InterviewReservationRequest;
import com.orv.api.domain.reservation.dto.RecapReservationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AmazonS3 amazonS3;

    private static final String testMemberId = "054c3e8a-3387-4eb3-ac8a-31a48221f192";

    private static final String testStoryboardId = "614c3e8a-3387-4eb3-ac8a-31a48221f192";

    private static final String testVideoId = "5d2add55-fa18-44de-bfc2-bb863222ffe0";

    private static String token;

    @BeforeEach
    public void setUp() {
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
        jdbcTemplate.update("DELETE FROM member");

        // member 테이블에 테스트 회원 데이터 삽입
        jdbcTemplate.update("INSERT INTO member (id, nickname, provider, social_id, email, profile_image_url, phone_number, birthday, gender, name) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                UUID.fromString(testMemberId), "testUser", "testProvider", "social123", "test@example.com", "http://example.com/profile.jpg", "01012345678", LocalDate.of(2000, 1, 1), "male", "Test User");

        // storyboard 테이블에 테스트 스토리보드 데이터 삽입
        jdbcTemplate.update("INSERT INTO storyboard (id, title, start_scene_id) VALUES (?, ?, ?)",
                UUID.fromString(testStoryboardId), "Test Storyboard", null);

        // video 테이블에 테스트 비디오 데이터 삽입
        jdbcTemplate.update("INSERT INTO video (id, storyboard_id, member_id, video_url, title, running_time, thumbnail_url, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, NOW())",
                UUID.fromString(testVideoId), UUID.fromString(testStoryboardId), UUID.fromString(testMemberId), "https://youtube.com", "Test Video", 324, "http://example.com/thumbnail.jpg");
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
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM recap_reservation", Integer.class);
        assertThat(count).isGreaterThan(0);
    }
}
