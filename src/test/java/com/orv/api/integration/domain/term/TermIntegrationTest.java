package com.orv.api.integration.domain.term;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orv.api.domain.auth.service.JwtTokenService;
import com.orv.api.domain.term.service.dto.TermAgreementForm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
public class TermIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenService jwtTokenProvider;
    private static final String testMemberId = "054c3e8a-3387-4eb3-ac8a-31a48221f192";
    private String token;


    @BeforeEach
    public void setUp() {
        // SecurityContext에 테스트 회원 ID 설정 (컨트롤러에서 SecurityContextHolder.getContext().getAuthentication().getName()로 사용)
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(testMemberId, null)
        );

        token = jwtTokenProvider.createToken(testMemberId, Map.of("provider", "google", "socialId", "12513412"));

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
                UUID.fromString(testMemberId), "testUser", "google", "12513412", "test@example.com", "http://example.com/profile.jpg", "01012345678", LocalDate.of(2000, 1, 1), "male", "Test User");
    }

    /**
     * POST /api/v0/term/agreement
     * - TermAgreementForm 데이터가 올바르게 저장되어, 생성된 agreement id와 클라이언트 IP가 DB에 반영되는지 확인
     */
    @Test
    public void testCreateAgreement() throws Exception {
        // given: TermAgreementForm 객체 생성
        TermAgreementForm form = new TermAgreementForm();
        form.setTerm("TEST_TERM");
        form.setValue("AGREE");

        String jsonRequest = objectMapper.writeValueAsString(form);

        // when: POST 요청 실행 (X-Forwarded-For 헤더로 클라이언트 IP 전달)
        String responseContent = mockMvc.perform(post("/api/v0/term/agreement")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Forwarded-For", "192.168.1.100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().is2xxSuccessful()) // ApiResponse.success 시 201 코드 반환
                .andExpect(jsonPath("$.statusCode").value(201))
                .andReturn().getResponse().getContentAsString();

        // then: 응답에서 반환된 agreement id 검증
        Map<String, Object> responseMap = objectMapper.readValue(responseContent, Map.class);
        assertThat(responseMap.get("data")).isNotNull();
        String agreementId = responseMap.get("data").toString();

        // DB에서 term_agreement 테이블에 해당 agreement id가 존재하는지 확인
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM term_agreement WHERE id = ?",
                Integer.class, UUID.fromString(agreementId)
        );
        assertThat(count).isEqualTo(1);

        // 추가로, 저장된 ip_address가 전달한 값과 동일한지 확인
        String storedIp = jdbcTemplate.queryForObject(
                "SELECT ip_address FROM term_agreement WHERE id = ?",
                String.class, UUID.fromString(agreementId)
        );
        assertThat(storedIp).isEqualTo("192.168.1.100");
    }
}