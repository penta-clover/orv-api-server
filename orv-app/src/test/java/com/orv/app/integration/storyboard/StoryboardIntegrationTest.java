package com.orv.app.integration.storyboard;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
public class StoryboardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    // 테스트용 데이터

    private static final String testTopicId = "ba4c3e8a-3387-4eb3-ac8a-31a48221f192";
    private static final String testMemberId = "054c3e8a-3387-4eb3-ac8a-31a48221f192";

    private static final String testStoryboardId = "614c3e8a-3387-4eb3-ac8a-31a48221f192";
    private static final String testSceneId = "164c3e8a-3387-4eb3-ac8a-31a48221f192";

    @BeforeEach
    public void setUp() {
        // 1. 테스트용 Storyboard 삽입
        UUID storyboardId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO storyboard (id, title, start_scene_id) VALUES (?, ?, ?)",
                UUID.fromString(testStoryboardId), "Test Storyboard", null);

        // 2. 테스트용 Scene 삽입 (preview 검증을 위해 QUESTION 타입 사용)
        UUID sceneId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO scene (id, name, scene_type, content, storyboard_id) VALUES (?, ?, ?, ?::json, ?)",
                UUID.fromString(testSceneId), "Test Scene", "QUESTION", "{\"key\":\"value\"}", UUID.fromString(testStoryboardId));

        // 3. storyboard_preview 테이블에 예제 데이터 삽입
        jdbcTemplate.update("INSERT INTO storyboard_preview (storyboard_id, examples) VALUES (?, array['example1','example2'])",
                UUID.fromString(testStoryboardId));

        // 4. 테스트용 Topic 삽입 및 storyboard_topic 연결
        UUID topicId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO topic (id, name, description, thumbnail_url) VALUES (?, ?, ?, ?)",
                UUID.fromString(testTopicId), "Test Topic", "Test Topic Description", "http://example.com/topic.jpg");
        jdbcTemplate.update("INSERT INTO storyboard_topic (storyboard_id, topic_id) VALUES (?, ?)",
                UUID.fromString(testStoryboardId), UUID.fromString(testTopicId));
    }

    /**
     * GET /api/v0/storyboard/{storyboardId}
     * - Storyboard의 상세 정보가 올바르게 반환되는지 검증
     */
    @Test
    public void testGetStoryboard() throws Exception {
        String url = "/api/v0/storyboard/" + testStoryboardId.toString();
        mockMvc.perform(get(url)
                        .with(user(testMemberId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.id").value(testStoryboardId.toString()))
                .andExpect(jsonPath("$.data.title").value("Test Storyboard"));
    }

    /**
     * GET /api/v0/storyboard/scene/{sceneId}
     * - Scene의 상세 정보가 반환되고, 호출 시 사용 기록이 업데이트되는지 검증
     */
    @Test
    public void testGetScene() throws Exception {
        String url = "/api/v0/storyboard/scene/" + testSceneId.toString();
        mockMvc.perform(get(url)
                        .with(user(testMemberId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.id").value(testSceneId.toString()))
                .andExpect(jsonPath("$.data.name").value("Test Scene"))
                .andExpect(jsonPath("$.data.sceneType").value("QUESTION"));

        // updateUsageHistory 호출 여부 검증: storyboard_usage_history 테이블에 기록이 생성되었는지 확인
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM storyboard_usage_history WHERE storyboard_id = ? AND member_id = ?",
                Integer.class, UUID.fromString(testStoryboardId), UUID.fromString(testMemberId)
        );
        assertThat(count).isGreaterThan(0);
    }

    /**
     * GET /api/v0/storyboard/{storyboardId}/preview
     * - 해당 Storyboard의 preview 정보를 반환하는지 검증 (QUESTION 타입 Scene 개수와 예제 배열 포함)
     */
    @Test
    public void testGetStoryboardPreview() throws Exception {
        String url = "/api/v0/storyboard/" + testStoryboardId.toString() + "/preview";
        mockMvc.perform(get(url)
                        .with(user(testMemberId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                // 반환된 데이터의 storyboardId와 questionCount 검증
                .andExpect(jsonPath("$.data.storyboardId").value(testStoryboardId.toString()))
                .andExpect(jsonPath("$.data.questionCount").value(1))
                // 예제 배열 검증
                .andExpect(jsonPath("$.data.questions").isArray())
                .andExpect(jsonPath("$.data.questions[0]").value("example1"))
                .andExpect(jsonPath("$.data.questions[1]").value("example2"));
    }

    /**
     * GET /api/v0/storyboard/{storyboardId}/topic/list
     * - 해당 Storyboard에 연결된 Topic 목록이 반환되는지 검증
     */
    @Test
    public void testGetTopicsOfStoryboard() throws Exception {
        String url = "/api/v0/storyboard/" + testStoryboardId.toString() + "/topic/list";
        mockMvc.perform(get(url)
                        .with(user(testMemberId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                // data가 배열이며, 미리 삽입한 Topic ID가 포함되어 있는지 확인
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[?(@.id=='" + testTopicId.toString() + "')]").exists());
    }
}
