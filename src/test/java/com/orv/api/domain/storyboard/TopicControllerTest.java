package com.orv.api.domain.storyboard;

import com.orv.api.domain.storyboard.dto.Storyboard;
import com.orv.api.domain.storyboard.dto.Topic;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TopicController.class)
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@AutoConfigureMockMvc(addFilters = false)
public class TopicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TopicRepository topicRepository;

    @Test
    public void testGetTopics() throws Exception {
        // given
        Topic topic = new Topic();
        topic.setId(UUID.fromString("1be07a77-4c5b-4661-b0d4-d80502fbea98"));
        topic.setName("죽음");
        topic.setDescription("죽음은 현존재에게 가장 고유하고 확실한 가능성이다. - 하이데거");
        List<Topic> topics = List.of(topic);

        when(topicRepository.findTopics()).thenReturn(topics);

        // when
        ResultActions resultActions = mockMvc.perform(get("/api/v0/topic/list"));

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("200"))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data[0].id").value(topic.getId().toString()))
                .andExpect(jsonPath("$.data[0].name").value("죽음"))
                .andExpect(jsonPath("$.data[0].description").value("죽음은 현존재에게 가장 고유하고 확실한 가능성이다. - 하이데거"))
                .andDo(document("topic/get-topics",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("statusCode").description("응답 상태 코드"),
                                fieldWithPath("message").description("응답 상태 메시지"),
                                fieldWithPath("data[].id").description("Topic의 ID"),
                                fieldWithPath("data[].name").description("Topic의 이름"),
                                fieldWithPath("data[].description").description("Topic의 설명")
                        )
                ));
    }


    @Test
    public void testGetNextStoryboard_whenStoryboardsExist() throws Exception {
        // given
        UUID topicId = UUID.fromString("1be07a77-4c5b-4661-b0d4-d80502fbea98");
        Storyboard storyboard = new Storyboard();
        storyboard.setId(UUID.fromString("e5895e70-7713-4a35-b12f-2521af77524b"));
        storyboard.setTitle("테스트 스토리보드");
        storyboard.setStartSceneId(UUID.fromString("50c4dfc2-8bec-4d77-849f-57462d50d393"));

        List<Storyboard> storyboards = List.of(storyboard);
        when(topicRepository.findStoryboardsByTopicId(topicId)).thenReturn(storyboards);

        // when
        ResultActions resultActions = mockMvc.perform(get("/api/v0/topic/{topicId}/storyboard/next", topicId.toString()));

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("200"))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.id").value(storyboard.getId().toString()))
                .andExpect(jsonPath("$.data.title").value(storyboard.getTitle()))
                .andExpect(jsonPath("$.data.startSceneId").value(storyboard.getStartSceneId().toString()))
                .andDo(document("topic/get-next-storyboard-success",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("topicId").description("Topic의 ID")
                        ),
                        responseFields(
                                fieldWithPath("statusCode").description("응답 상태 코드"),
                                fieldWithPath("message").description("응답 상태 메시지"),
                                fieldWithPath("data.id").description("Storyboard의 ID"),
                                fieldWithPath("data.title").description("Storyboard의 제목"),
                                fieldWithPath("data.startSceneId").description("Storyboard의 시작 Scene ID")
                        )
                ));
    }
}
