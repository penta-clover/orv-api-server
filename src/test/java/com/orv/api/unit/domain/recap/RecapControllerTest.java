package com.orv.api.unit.domain.recap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orv.api.domain.recap.controller.RecapController;
import com.orv.api.domain.recap.controller.dto.*;
import com.orv.api.domain.recap.orchestrator.RecapOrchestrator;
import com.orv.api.domain.reservation.controller.InterviewReservationController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecapController.class)
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@AutoConfigureMockMvc(addFilters = false)
public class RecapControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RecapOrchestrator recapOrchestrator;

    @Test
    @WithMockUser(username = "054c3e8a-3387-4eb3-ac8a-31a48221f192")
    public void testReserveRecap() throws Exception {
        // given
        RecapReservationRequest request = new RecapReservationRequest();
        request.setVideoId("e5895e70-7713-4a35-b12f-2521af77524b");
        request.setScheduledAt(ZonedDateTime.parse("2028-03-22T00:36:00+09:00"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        String formattedTime = request.getScheduledAt().format(formatter);

        String generatedId = "d23abc70-7713-4a35-b12f-2521af77524b";
        RecapReservationResponse response = new RecapReservationResponse(
                UUID.fromString(generatedId),
                UUID.fromString("054c3e8a-3387-4eb3-ac8a-31a48221f192"),
                UUID.fromString(request.getVideoId()),
                request.getScheduledAt().toLocalDateTime(),
                LocalDateTime.now()
        );

        when(recapOrchestrator.reserveRecap(any(), any(), any())).thenReturn(Optional.of(response));

        // when
        ResultActions resultActions = mockMvc.perform(post("/api/v0/reservation/recap/video")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(generatedId))
                .andExpect(jsonPath("$.data.memberId").value("054c3e8a-3387-4eb3-ac8a-31a48221f192"))
                .andExpect(jsonPath("$.data.videoId").value(request.getVideoId()))
                .andExpect(jsonPath("$.data.scheduledAt").value(formattedTime))
                .andExpect(jsonPath("$.data.createdAt").isString())
                .andDo(
                        document("reservation/recap-success",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestFields(
                                        fieldWithPath("videoId").description("에약할 비디오 ID"),
                                        fieldWithPath("scheduledAt").description("예약할 날짜와 시간 (ISO-8601, 시간대 포함)")
                                ),
                                responseFields(
                                        fieldWithPath("statusCode").description("응답 상태 코드"),
                                        fieldWithPath("message").description("응답 상태 메시지"),
                                        fieldWithPath("data.id").description("예약 ID"),
                                        fieldWithPath("data.memberId").description("예약한 회원 ID"),
                                        fieldWithPath("data.videoId").description("예약한 비디오 ID"),
                                        fieldWithPath("data.scheduledAt").description("예약 대상 일시"),
                                        fieldWithPath("data.createdAt").description("예약을 생성한 일시")
                                )
                        )
                );
    }


    @Test
    @WithMockUser(username = "054c3e8a-3387-4eb3-ac8a-31a48221f192")
    public void testGetRecapResultById() throws Exception {
        // given
        UUID recapResultId = UUID.fromString("a1b2c3d4-e5f6-7890-1234-567890abcdef");
        UUID sceneId1 = UUID.fromString("b1b2c3d4-e5f6-7890-1234-567890abcdef");
        UUID sceneId2 = UUID.fromString("c1c2d3e4-f5a6-7890-1234-567890abcdef");

        RecapAnswerSummaryResponse summary1 = new RecapAnswerSummaryResponse(sceneId1, "Question for scene 1", "Summary for scene 1");
        RecapAnswerSummaryResponse summary2 = new RecapAnswerSummaryResponse(sceneId2, "Question for scene 2", "Summary for scene 2");

        RecapResultResponse mockResponse = new RecapResultResponse(
                recapResultId,
                ZonedDateTime.now().toOffsetDateTime(),
                Arrays.asList(summary1, summary2)
        );

        when(recapOrchestrator.getRecapResult(any(UUID.class))).thenReturn(Optional.of(mockResponse));

        // when
        ResultActions resultActions = mockMvc.perform(get("/api/v0/reservation/recap/{recapResultId}/result", recapResultId)
                .accept(MediaType.APPLICATION_JSON));

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recapResultId").value(recapResultId.toString()))
                .andExpect(jsonPath("$.data.answerSummaries[0].sceneId").value(summary1.getSceneId().toString()))
                .andExpect(jsonPath("$.data.answerSummaries[0].question").value(summary1.getQuestion()))
                .andExpect(jsonPath("$.data.answerSummaries[0].answerSummary").value(summary1.getAnswerSummary()))
                .andExpect(jsonPath("$.data.answerSummaries[1].sceneId").value(summary2.getSceneId().toString()))
                .andExpect(jsonPath("$.data.answerSummaries[1].question").value(summary2.getQuestion()))
                .andExpect(jsonPath("$.data.answerSummaries[1].answerSummary").value(summary2.getAnswerSummary()))
                .andDo(document("recap/get-recap-result-by-id",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("recapResultId").description("조회할 리캡 결과 ID")
                        ),
                        responseFields(
                                fieldWithPath("statusCode").description("응답 상태 코드"),
                                fieldWithPath("message").description("응답 상태 메시지"),
                                fieldWithPath("data.recapResultId").description("리캡 결과 ID"),
                                fieldWithPath("data.createdAt").description("리캡 결과 생성 일시"),
                                fieldWithPath("data.answerSummaries[].sceneId").description("요약된 씬 ID"),
                                fieldWithPath("data.answerSummaries[].question").description("씬 질문 내용"),
                                fieldWithPath("data.answerSummaries[].answerSummary").description("씬 답변 요약 내용")
                        )
                ));
    }

    @Test
    @WithMockUser(username = "054c3e8a-3387-4eb3-ac8a-31a48221f192")
    public void testGetRecapAudio() throws Exception {
        // given
        UUID recapReservationId = UUID.fromString("a1b2c3d4-e5f6-7890-1234-567890abcdef");
        UUID audioId = UUID.fromString("b1b2c3d4-e5f6-7890-1234-567890abcdef");
        String audioUrl = "https://s3.amazonaws.com/test-audio.opus";
        Integer runningTime = 324;
        OffsetDateTime createdAt = OffsetDateTime.now();

        RecapAudioResponse mockResponse = new RecapAudioResponse(
                audioId,
                audioUrl,
                runningTime,
                createdAt
        );

        when(recapOrchestrator.getRecapAudio(any(UUID.class))).thenReturn(Optional.of(mockResponse));

        // when
        ResultActions resultActions = mockMvc.perform(get("/api/v0/reservation/recap/{recapReservationId}/audio", recapReservationId)
                .accept(MediaType.APPLICATION_JSON));

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.audioId").value(audioId.toString()))
                .andExpect(jsonPath("$.data.audioUrl").value(audioUrl))
                .andExpect(jsonPath("$.data.runningTime").value(runningTime))
                .andExpect(jsonPath("$.data.createdAt").isString())
                .andDo(document("recap/get-recap-audio-by-id",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("recapReservationId").description("조회할 리캡 예약 ID")
                        ),
                        responseFields(
                                fieldWithPath("statusCode").description("응답 상태 코드"),
                                fieldWithPath("message").description("응답 상태 메시지"),
                                fieldWithPath("data.audioId").description("오디오 ID"),
                                fieldWithPath("data.audioUrl").description("오디오 S3 URL"),
                                fieldWithPath("data.runningTime").description("오디오 재생 시간 (초)"),
                                fieldWithPath("data.createdAt").description("오디오 생성 일시")
                        )
                ));
    }
}
