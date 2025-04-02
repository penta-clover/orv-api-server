package com.orv.api.domain.storyboard;

import com.orv.api.domain.storyboard.dto.Scene;
import com.orv.api.domain.storyboard.dto.Storyboard;
import com.orv.api.domain.storyboard.dto.Topic;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StoryboardController.class)
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@AutoConfigureMockMvc(addFilters = false)
public class StoryboardControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StoryboardRepository storyboardRepository;

    @Test
    public void testGetStoryboard_whenStoryboardExists() throws Exception {
        // given
        Storyboard storyboard = new Storyboard();
        storyboard.setTitle("test title");
        storyboard.setId(UUID.fromString("e5895e70-7713-4a35-b12f-2521af77524b"));
        storyboard.setStartSceneId(UUID.fromString("50c4dfc2-8bec-4d77-849f-57462d50d393"));

        when(storyboardRepository.findById(storyboard.getId())).thenReturn(Optional.of(storyboard));

        // when
        ResultActions resultActions = mockMvc.perform(get("/api/v0/storyboard/{storyboardId}", storyboard.getId()));

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(storyboard.getId().toString()))
                .andExpect(jsonPath("$.data.title").value(storyboard.getTitle()))
                .andExpect(jsonPath("$.data.startSceneId").value(storyboard.getStartSceneId().toString()))
                .andDo(document("storyboard/get-storyboard-success",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("storyboardId").description("Storyboard의 ID")
                        ),
                        responseFields(
                                fieldWithPath("statusCode").description("응답 상태 코드"),
                                fieldWithPath("message").description("응답 상태 메시지"),
                                fieldWithPath("data.id").description("Storyboard의 ID"),
                                fieldWithPath("data.title").description("Storyboard의 제목"),
                                fieldWithPath("data.startSceneId").description("시작 Scene의 ID")
                        )
                ));
    }


    @Test
    public void testGetStoryboard_whenStoryboardNotExists() throws Exception {
        // given
        UUID uuid = UUID.randomUUID();
        when(storyboardRepository.findById(uuid)).thenReturn(Optional.empty());

        // when
        ResultActions resultActions = mockMvc.perform(get("/api/v0/storyboard/{storyboardId}", uuid.toString()));

        // then
        resultActions.andExpect(jsonPath("$.statusCode").value(404));
    }

    @Test
    @WithMockUser(username = "054c3e8a-3387-4eb3-ac8a-31a48221f192")
    public void testGetScene_whenSceneExists() throws Exception {
        // given
        Scene scene = new Scene();
        scene.setId(UUID.fromString("50c4dfc2-8bec-4d77-849f-57462d50d393"));
        scene.setName("테스트 3");
        scene.setSceneType("QUESTION");
        scene.setContent("{ \"question\": \"당신에게 가장 소중한 것은 무엇인가요?\", \"nextSceneId\": \"" + UUID.randomUUID() + "\" }");
        scene.setStoryboardId(UUID.randomUUID());

        when(storyboardRepository.findSceneById(scene.getId())).thenReturn(Optional.of(scene));

        // when
        ResultActions resultActions = mockMvc.perform(get("/api/v0/storyboard/scene/{sceneId}", scene.getId()));

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(scene.getId().toString()))
                .andExpect(jsonPath("$.data.name").value(scene.getName()))
                .andExpect(jsonPath("$.data.sceneType").value(scene.getSceneType()))
                .andExpect(jsonPath("$.data.content").value(scene.getContent()))
                .andExpect(jsonPath("$.data.storyboardId").value(scene.getStoryboardId().toString()))
                .andDo(document("storyboard/get-scene-success",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("sceneId").description("Scene의 ID")
                        ),
                        responseFields(
                                fieldWithPath("statusCode").description("응답 상태 코드"),
                                fieldWithPath("message").description("응답 상태 메시지"),
                                fieldWithPath("data.id").description("Scene의 ID"),
                                fieldWithPath("data.name").description("Scene의 이름"),
                                fieldWithPath("data.sceneType").description("Scene의 타입"),
                                fieldWithPath("data.content").description("Scene의 내용 (JSON 형식)"),
                                fieldWithPath("data.storyboardId").description("관련 Storyboard의 ID")
                        )
                ));
    }

    @Test
    @WithMockUser(username = "054c3e8a-3387-4eb3-ac8a-31a48221f192")
    public void testGetScene_whenSceneNotExists() throws Exception {
        // given
        UUID uuid = UUID.randomUUID();
        when(storyboardRepository.findSceneById(uuid)).thenReturn(Optional.empty());

        // when
        ResultActions resultActions = mockMvc.perform(get("/api/v0/storyboard/scene/{sceneId}", uuid.toString()));

        // then
        resultActions.andExpect(jsonPath("$.statusCode").value(404));
    }

    @Test
    public void testGetStoryboardPreview_whenDataExists() throws Exception {
        // given
        UUID storyboardId = UUID.fromString("e5895e70-7713-4a35-b12f-2521af77524b");

        // Scene 목록 생성 - QUESTION 타입인 Scene 3개로 questionCount가 3이 되도록 함
        Scene scene1 = new Scene();
        scene1.setSceneType("QUESTION");
        Scene scene2 = new Scene();
        scene2.setSceneType("QUESTION");
        Scene scene3 = new Scene();
        scene3.setSceneType("QUESTION");
        List<Scene> scenes = List.of(scene1, scene2, scene3);
        when(storyboardRepository.findScenesByStoryboardId(storyboardId)).thenReturn(Optional.of(scenes));

        // storyboard_preview의 예시 데이터 - 예제 응답에서 questions 배열에 해당
        String[] examples = new String[]{"나는 어떤 사람으로 기억되고 싶나요?", "유언장에는 어떤 내용을 적고 싶나요?"};
        when(storyboardRepository.getStoryboardPreview(storyboardId)).thenReturn(Optional.of(examples));

        // when
        ResultActions resultActions = mockMvc.perform(get("/api/v0/storyboard/{storyboardId}/preview", storyboardId.toString()));

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("200"))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.storyboardId").value(storyboardId.toString()))
                .andExpect(jsonPath("$.data.questionCount").value(scenes.size()))
                .andExpect(jsonPath("$.data.questions[0]").value(examples[0]))
                .andExpect(jsonPath("$.data.questions[1]").value(examples[1]))
                .andDo(document("storyboard/get-storyboard-preview-success",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("storyboardId").description("Storyboard의 ID")
                        ),
                        responseFields(
                                fieldWithPath("statusCode").description("응답 상태 코드"),
                                fieldWithPath("message").description("응답 상태 메시지"),
                                fieldWithPath("data.storyboardId").description("Storyboard의 ID"),
                                fieldWithPath("data.questionCount").description("스토리보드에 포함된 질문 개수"),
                                fieldWithPath("data.questions").description("예시 질문 목록")
                        )
                ));
    }

    @Test
    public void testGetTopicsOfStoryboard() throws Exception {
        // given
        UUID storyboardId = UUID.fromString("e5895e70-7713-4a35-b12f-2521af77524b");
        Optional<List<Topic>> topics = Optional.of(List.of(
                new Topic(UUID.randomUUID(), "topic1", "topic1 description", "https://thumbnail.com/url1"),
                new Topic(UUID.randomUUID(), "topic2", "topic2 description", "https://thumbnail.com/url2"))
        );

        when(storyboardRepository.findTopicsOfStoryboard(storyboardId)).thenReturn(topics);

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
                .andExpect(jsonPath("$.data[1].id").exists())
                .andExpect(jsonPath("$.data[1].name").value("topic2"))
                .andExpect(jsonPath("$.data[1].description").value("topic2 description"))
                .andExpect(jsonPath("$.data[1].thumbnailUrl").value("https://thumbnail.com/url2"))
                .andDo(document("storyboard/get-topics-of-storyboard",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("storyboardId").description("Storyboard의 ID")
                        ),
                        responseFields(
                                fieldWithPath("statusCode").description("응답 상태 코드"),
                                fieldWithPath("message").description("응답 상태 메시지"),
                                fieldWithPath("data[].id").description("Topic의 ID"),
                                fieldWithPath("data[].name").description("Topic의 이름"),
                                fieldWithPath("data[].description").description("Topic의 설명"),
                                fieldWithPath("data[].thumbnailUrl").description("Topic의 썸네일 URL")
                        )
                ));

    }
}