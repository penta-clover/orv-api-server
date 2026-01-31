package com.orv.storyboard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.orv.storyboard.orchestrator.dto.StoryboardResponse;
import com.orv.storyboard.orchestrator.dto.TopicResponse;
import com.orv.storyboard.orchestrator.TopicOrchestrator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class TopicControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TopicOrchestrator topicOrchestrator;

    @InjectMocks
    private TopicController topicController;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mockMvc = MockMvcBuilders.standaloneSetup(topicController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    public void testGetTopics() throws Exception {
        // given
        UUID topicId = UUID.fromString("1be07a77-4c5b-4661-b0d4-d80502fbea98");
        TopicResponse topic = new TopicResponse(
                topicId,
                "죽음",
                "죽음은 현존재에게 가장 고유하고 확실한 가능성이다. - 하이데거",
                "https://www.naver.com/favicon.ico",
                Collections.emptyList()
        );
        List<TopicResponse> topics = List.of(topic);

        when(topicOrchestrator.getTopicsByCategory(any())).thenReturn(topics);

        // when
        ResultActions resultActions = mockMvc.perform(get("/api/v0/topic/list"));

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("200"))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data[0].id").value(topicId.toString()))
                .andExpect(jsonPath("$.data[0].name").value("죽음"))
                .andExpect(jsonPath("$.data[0].description").value("죽음은 현존재에게 가장 고유하고 확실한 가능성이다. - 하이데거"))
                .andExpect(jsonPath("$.data[0].thumbnailUrl").value("https://www.naver.com/favicon.ico"))
                .andExpect(jsonPath("$.data[0].hashtags").isEmpty());
    }


    @Test
    public void testGetNextStoryboard_whenStoryboardsExist() throws Exception {
        // given
        UUID topicId = UUID.fromString("1be07a77-4c5b-4661-b0d4-d80502fbea98");
        UUID storyboardId = UUID.fromString("e5895e70-7713-4a35-b12f-2521af77524b");
        UUID startSceneId = UUID.fromString("50c4dfc2-8bec-4d77-849f-57462d50d393");
        StoryboardResponse storyboard = new StoryboardResponse(storyboardId, "테스트 스토리보드", startSceneId);

        when(topicOrchestrator.getNextStoryboard(topicId)).thenReturn(Optional.of(storyboard));

        // when
        ResultActions resultActions = mockMvc.perform(get("/api/v0/topic/{topicId}/storyboard/next", topicId.toString()));

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("200"))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.id").value(storyboardId.toString()))
                .andExpect(jsonPath("$.data.title").value(storyboard.getTitle()))
                .andExpect(jsonPath("$.data.startSceneId").value(startSceneId.toString()));
    }

    @Test
    public void testGetTopic() throws Exception {
        // given
        UUID topicId = UUID.fromString("1be07a77-4c5b-4661-b0d4-d80502fbea98");
        TopicResponse topic = new TopicResponse(
                topicId,
                "죽음",
                "죽음은 현존재에게 가장 고유하고 확실한 가능성이다. - 하이데거",
                "https://www.naver.com/favicon.ico",
                null
        );

        when(topicOrchestrator.getTopic(topicId)).thenReturn(Optional.of(topic));

        // when
        ResultActions resultActions = mockMvc.perform(get("/api/v0/topic/{topicId}", topicId.toString()));

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("200"))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.id").value(topic.getId().toString()))
                .andExpect(jsonPath("$.data.name").value("죽음"))
                .andExpect(jsonPath("$.data.description").value("죽음은 현존재에게 가장 고유하고 확실한 가능성이다. - 하이데거"))
                .andExpect(jsonPath("$.data.thumbnailUrl").value("https://www.naver.com/favicon.ico"))
                .andExpect(jsonPath("$.data.hashtags").isEmpty());
    }
}
