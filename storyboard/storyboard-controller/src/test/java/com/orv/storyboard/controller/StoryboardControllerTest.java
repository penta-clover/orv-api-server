package com.orv.storyboard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.orv.storyboard.orchestrator.dto.*;
import com.orv.storyboard.orchestrator.StoryboardOrchestrator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class StoryboardControllerTest {
    private MockMvc mockMvc;

    @Mock
    private StoryboardOrchestrator storyboardOrchestrator;

    @InjectMocks
    private StoryboardController storyboardController;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mockMvc = MockMvcBuilders.standaloneSetup(storyboardController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("054c3e8a-3387-4eb3-ac8a-31a48221f192", null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    public void testGetStoryboard_whenStoryboardExists() throws Exception {
        // given
        UUID storyboardId = UUID.fromString("e5895e70-7713-4a35-b12f-2521af77524b");
        UUID startSceneId = UUID.fromString("50c4dfc2-8bec-4d77-849f-57462d50d393");
        StoryboardResponse storyboard = new StoryboardResponse(storyboardId, "test title", startSceneId);

        when(storyboardOrchestrator.getStoryboard(storyboardId)).thenReturn(Optional.of(storyboard));

        // when
        ResultActions resultActions = mockMvc.perform(get("/api/v0/storyboard/{storyboardId}", storyboardId));

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(storyboardId.toString()))
                .andExpect(jsonPath("$.data.title").value(storyboard.getTitle()))
                .andExpect(jsonPath("$.data.startSceneId").value(startSceneId.toString()));
    }


    @Test
    public void testGetStoryboard_whenStoryboardNotExists() throws Exception {
        // given
        UUID uuid = UUID.randomUUID();
        when(storyboardOrchestrator.getStoryboard(uuid)).thenReturn(Optional.empty());

        // when
        ResultActions resultActions = mockMvc.perform(get("/api/v0/storyboard/{storyboardId}", uuid.toString()));

        // then
        resultActions.andExpect(jsonPath("$.statusCode").value(404));
    }

    @Test
    public void testGetScene_whenSceneExists() throws Exception {
        // given
        UUID sceneId = UUID.fromString("50c4dfc2-8bec-4d77-849f-57462d50d393");
        UUID storyboardId = UUID.randomUUID();
        String content = "{ \"question\": \"당신에게 가장 소중한 것은 무엇인가요?\", \"nextSceneId\": \"" + UUID.randomUUID() + "\" }";
        SceneResponse scene = new SceneResponse(sceneId, "테스트 3", "QUESTION", content, storyboardId);

        when(storyboardOrchestrator.getSceneAndUpdateUsageHistory(eq(sceneId), any(UUID.class))).thenReturn(Optional.of(scene));

        // when
        ResultActions resultActions = mockMvc.perform(get("/api/v0/storyboard/scene/{sceneId}", sceneId));

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(sceneId.toString()))
                .andExpect(jsonPath("$.data.name").value(scene.getName()))
                .andExpect(jsonPath("$.data.sceneType").value(scene.getSceneType()))
                .andExpect(jsonPath("$.data.content").value(scene.getContent()))
                .andExpect(jsonPath("$.data.storyboardId").value(storyboardId.toString()));
    }

    @Test
    public void testGetScene_whenSceneNotExists() throws Exception {
        // given
        UUID uuid = UUID.randomUUID();
        when(storyboardOrchestrator.getSceneAndUpdateUsageHistory(eq(uuid), any(UUID.class))).thenReturn(Optional.empty());

        // when
        ResultActions resultActions = mockMvc.perform(get("/api/v0/storyboard/scene/{sceneId}", uuid.toString()));

        // then
        resultActions.andExpect(jsonPath("$.statusCode").value(404));
    }

    @Test
    public void testGetStoryboardPreview_whenDataExists() throws Exception {
        // given
        UUID storyboardId = UUID.fromString("e5895e70-7713-4a35-b12f-2521af77524b");

        List<String> questions = List.of("나는 어떤 사람으로 기억되고 싶나요?", "유언장에는 어떤 내용을 적고 싶나요?");
        StoryboardPreviewResponse previewResponse = new StoryboardPreviewResponse(storyboardId, 3, questions);
        when(storyboardOrchestrator.getStoryboardPreview(storyboardId)).thenReturn(Optional.of(previewResponse));

        // when
        ResultActions resultActions = mockMvc.perform(get("/api/v0/storyboard/{storyboardId}/preview", storyboardId.toString()));

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("200"))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.storyboardId").value(storyboardId.toString()))
                .andExpect(jsonPath("$.data.questionCount").value(3))
                .andExpect(jsonPath("$.data.questions[0]").value(questions.get(0)))
                .andExpect(jsonPath("$.data.questions[1]").value(questions.get(1)));
    }

    @Test
    public void testGetTopicsOfStoryboard() throws Exception {
        // given
        UUID storyboardId = UUID.fromString("e5895e70-7713-4a35-b12f-2521af77524b");
        Optional<List<TopicResponse>> topics = Optional.of(List.of(
                new TopicResponse(UUID.randomUUID(), "topic1", "topic1 description", "https://thumbnail.com/url1", Collections.emptyList()),
                new TopicResponse(UUID.randomUUID(), "topic2", "topic2 description", "https://thumbnail.com/url2", Collections.emptyList()))
        );

        when(storyboardOrchestrator.getTopicsOfStoryboard(storyboardId)).thenReturn(topics);

        // when
        ResultActions resultActions = mockMvc.perform(get("/api/v0/storyboard/{storyboardId}/topic/list", storyboardId.toString()));

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("200"))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data[0].id").exists())
                .andExpect(jsonPath("$.data[0].name").value("topic1"))
                .andExpect(jsonPath("$.data[0].description").value("topic1 description"))
                .andExpect(jsonPath("$.data[0].thumbnailUrl").value("https://thumbnail.com/url1"))
                .andExpect(jsonPath("$.data[0].hashtags").isArray())
                .andExpect(jsonPath("$.data[1].id").exists())
                .andExpect(jsonPath("$.data[1].name").value("topic2"))
                .andExpect(jsonPath("$.data[1].description").value("topic2 description"))
                .andExpect(jsonPath("$.data[1].thumbnailUrl").value("https://thumbnail.com/url2"))
                .andExpect(jsonPath("$.data[1].hashtags").isArray());

    }
}
