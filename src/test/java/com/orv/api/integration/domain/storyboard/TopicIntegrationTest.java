package com.orv.api.integration.domain.storyboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orv.api.domain.auth.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev") // dev 프로파일의 DB 설정 사용
@AutoConfigureMockMvc
@Transactional
public class TopicIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;


    private static final String testTopicId = "ba4c3e8a-3387-4eb3-ac8a-31a48221f192";
    private static final String testMemberId = "054c3e8a-3387-4eb3-ac8a-31a48221f192";

    private static final String testStoryboardId = "614c3e8a-3387-4eb3-ac8a-31a48221f192";
    private String token;

    @BeforeEach
    public void setUp() {
        // 테스트용 Topic 삽입
        String insertTopicSql = "INSERT INTO topic (id, name, description, thumbnail_url) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(insertTopicSql, UUID.fromString(testTopicId), "Integration Test Topic", "Integration Test Description", "http://example.com/test-thumbnail.jpg");

        // 테스트용 Storyboard 삽입;
        String insertStoryboardSql = "INSERT INTO storyboard (id, title, start_scene_id) VALUES (?, ?, ?)";
        jdbcTemplate.update(insertStoryboardSql, UUID.fromString(testStoryboardId), "Integration Test Storyboard", null);

        // Storyboard와 Topic을 연결하는 storyboard_topic 삽입
        String insertStoryboardTopicSql = "INSERT INTO storyboard_topic (storyboard_id, topic_id) VALUES (?, ?)";
        jdbcTemplate.update(insertStoryboardTopicSql, UUID.fromString(testStoryboardId), UUID.fromString(testTopicId));

        token = jwtTokenProvider.createToken(testMemberId, Map.of("provider", "google", "socialId", "12513412"));
    }

    /**
     * GET /api/v0/topic/list
     * - 등록된 Topic 목록이 반환되는지 확인
     */
    @Test
    public void testGetTopics() throws Exception {
        mockMvc.perform(get("/api/v0/topic/list")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data").isArray())
                // 미리 삽입한 testTopicId가 포함되어 있는지 JSONPath로 검증
                .andExpect(jsonPath("$.data[?(@.id == '" + testTopicId.toString() + "')]").exists());
    }

    /**
     * GET /api/v0/topic/{topicId}/storyboard/next
     * - 특정 Topic에 연결된 Storyboard 목록 중 첫 번째 항목이 반환되는지 확인
     */
    @Test
    public void testGetNextStoryboard() throws Exception {
        String url = "/api/v0/topic/" + testTopicId.toString() + "/storyboard/next";
        mockMvc.perform(get(url).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                // 반환된 Storyboard의 id와 title이 미리 삽입한 데이터와 일치하는지 확인
                .andExpect(jsonPath("$.data.id").value(testStoryboardId.toString()))
                .andExpect(jsonPath("$.data.title").value("Integration Test Storyboard"));
    }

    /**
     * GET /api/v0/topic/{topicId}
     * - 특정 Topic의 상세 정보가 반환되는지 확인
     */
    @Test
    public void testGetTopic() throws Exception {
        String url = "/api/v0/topic/" + testTopicId.toString();
        mockMvc.perform(get(url)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                // 반환된 Topic의 정보가 미리 삽입한 값과 일치하는지 확인
                .andExpect(jsonPath("$.data.id").value(testTopicId.toString()))
                .andExpect(jsonPath("$.data.name").value("Integration Test Topic"))
                .andExpect(jsonPath("$.data.description").value("Integration Test Description"))
                .andExpect(jsonPath("$.data.thumbnailUrl").value("http://example.com/test-thumbnail.jpg"));
    }
}
