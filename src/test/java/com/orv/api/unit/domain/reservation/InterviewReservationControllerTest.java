package com.orv.api.unit.domain.reservation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orv.api.domain.recap.controller.dto.RecapAnswerSummaryResponse;
import com.orv.api.domain.recap.controller.dto.RecapAudioResponse;
import com.orv.api.domain.recap.controller.dto.RecapReservationRequest;
import com.orv.api.domain.recap.controller.dto.RecapReservationResponse;
import com.orv.api.domain.recap.controller.dto.RecapResultResponse;
import com.orv.api.domain.recap.orchestrator.RecapOrchestrator;
import com.orv.api.domain.reservation.controller.InterviewReservationController;
import com.orv.api.domain.reservation.orchestrator.InterviewReservationOrchestrator;
import com.orv.api.domain.reservation.controller.dto.*;

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
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;


@WebMvcTest(InterviewReservationController.class)
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@AutoConfigureMockMvc(addFilters = false)
public class InterviewReservationControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InterviewReservationOrchestrator interviewreservationOrchestrator;

    @Test
    @WithMockUser(username = "054c3e8a-3387-4eb3-ac8a-31a48221f192")
    public void testReserveInterview() throws Exception {
        // given
        InterviewReservationRequest request = new InterviewReservationRequest();
        request.setStoryboardId("e5895e70-7713-4a35-b12f-2521af77524b");
        request.setReservedAt(ZonedDateTime.parse("2028-03-22T00:36:00+09:00"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        String formattedTime = request.getReservedAt().format(formatter);

        String generatedId = "e5895e70-7713-4a35-b12f-2521af77524b";
        UUID memberId = UUID.fromString("054c3e8a-3387-4eb3-ac8a-31a48221f192");
        UUID storyboardId = UUID.fromString(request.getStoryboardId());

        InterviewReservationResponse response = new InterviewReservationResponse(
            UUID.fromString(generatedId),
            memberId,
            storyboardId,
            request.getReservedAt().toLocalDateTime(),
            LocalDateTime.now()
        );

        when(interviewreservationOrchestrator.reserveInterview(any(), any(), any())).thenReturn(Optional.of(response));

        // when
        ResultActions resultActions = mockMvc.perform(post("/api/v0/reservation/interview")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(generatedId))
                .andExpect(jsonPath("$.data.memberId").value("054c3e8a-3387-4eb3-ac8a-31a48221f192"))
                .andExpect(jsonPath("$.data.storyboardId").value(request.getStoryboardId()))
                .andExpect(jsonPath("$.data.scheduledAt").value(formattedTime))
                .andExpect(jsonPath("$.data.createdAt").isString())
                .andDo(
                        document("reservation/interview-success",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestFields(
                                        fieldWithPath("storyboardId").description("에약할 스토리보드 ID"),
                                        fieldWithPath("reservedAt").description("예약할 날짜와 시간 (ISO-8601, 시간대 포함)")
                                ),
                                responseFields(
                                        fieldWithPath("statusCode").description("응답 상태 코드"),
                                        fieldWithPath("message").description("응답 상태 메시지"),
                                        fieldWithPath("data.id").description("예약 ID"),
                                        fieldWithPath("data.memberId").description("예약한 회원 ID"),
                                        fieldWithPath("data.storyboardId").description("예약한 스토리보드 ID"),
                                        fieldWithPath("data.scheduledAt").description("예약 대상 일시"),
                                        fieldWithPath("data.createdAt").description("예약을 생성한 일시")
                                )
                        )
                );
    }

    @Test
    @WithMockUser(username = "054c3e8a-3387-4eb3-ac8a-31a48221f192")
    public void testGetReservedInterviews() throws Exception {
        // given
        InterviewReservationResponse response = new InterviewReservationResponse(
                UUID.fromString("e5895e70-7713-4a32-b15f-2521af77524b"),
                UUID.fromString("054c3e8a-3387-4eb3-ac8a-31a48221f192"),
                UUID.fromString("e5895e70-7713-4a35-b12f-2521af77524b"),
                LocalDateTime.now().plusHours(5),
                LocalDateTime.now()
        );

        when(interviewreservationOrchestrator.getForwardInterviews(any(), any())).thenReturn(Optional.of(List.of(response)));

        // when
        ResultActions resultActions = mockMvc.perform(get("/api/v0/reservation/interview/forward")
                .param("from", "2025-03-29T12:00:00+09:00"));

        // then
        resultActions.andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.data[0].id").value("e5895e70-7713-4a32-b15f-2521af77524b"))
                .andExpect(jsonPath("$.data[0].memberId").value("054c3e8a-3387-4eb3-ac8a-31a48221f192"))
                .andExpect(jsonPath("$.data[0].storyboardId").value("e5895e70-7713-4a35-b12f-2521af77524b"))
                .andExpect(jsonPath("$.data[0].scheduledAt").isString())
                .andExpect(jsonPath("$.data[0].createdAt").isString())
                .andDo(
                        document("reservation/interview-forward",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                responseFields(
                                        fieldWithPath("statusCode").description("응답 상태 코드"),
                                        fieldWithPath("message").description("응답 상태 메시지"),
                                        fieldWithPath("data[].id").description("예약 ID"),
                                        fieldWithPath("data[].memberId").description("예약한 회원 ID"),
                                        fieldWithPath("data[].storyboardId").description("예약한 스토리보드 ID"),
                                        fieldWithPath("data[].scheduledAt").description("예약 대상 일시"),
                                        fieldWithPath("data[].createdAt").description("예약을 생성한 일시")
                                )
                        )
                );
    }

    @Test
    @WithMockUser(username = "054c3e8a-3387-4eb3-ac8a-31a48221f192")
    public void testDoneInterview() throws Exception {
        // given
        UUID interviewId = UUID.fromString("e5895e70-7713-4a32-b15f-2521af77524b");
        when(interviewreservationOrchestrator.markInterviewAsDone(any())).thenReturn(true);

        // when
        ResultActions resultActions = mockMvc.perform(patch("/api/v0/reservation/interview/{interviewId}/done", interviewId));

        // then
        resultActions.andExpect(status().isOk())
                .andDo(
                        document("reservation/interview-done",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                responseFields(
                                        fieldWithPath("statusCode").description("응답 상태 코드"),
                                        fieldWithPath("message").description("응답 상태 메시지"),
                                        fieldWithPath("data").description("null")
                                )
                        )
                );
    }


    @Test
    @WithMockUser(username = "054c3e8a-3387-4eb3-ac8a-31a48221f192")
    public void testGetInterviewReservationById() throws Exception {
        // given
        UUID reservationId = UUID.fromString("e5895e70-7713-4a32-b15f-2521af77524b");
        UUID memberId = UUID.fromString("054c3e8a-3387-4eb3-ac8a-31a48221f192");
        UUID storyboardId = UUID.fromString("e5895e70-7713-4a35-b12f-2521af77524b");
        LocalDateTime scheduledAt = LocalDateTime.now().plusDays(1);
        LocalDateTime createdAt = LocalDateTime.now();
        
        InterviewReservationResponse response = new InterviewReservationResponse(reservationId, memberId, storyboardId, scheduledAt, createdAt);

        when(interviewreservationOrchestrator.getInterviewReservationById(any())).thenReturn(Optional.of(response));

        // when
        ResultActions resultActions = mockMvc.perform(get("/api/v0/reservation/interview/{reservationId}", reservationId));

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(reservationId.toString()))
                .andExpect(jsonPath("$.data.memberId").value(memberId.toString()))
                .andExpect(jsonPath("$.data.storyboardId").value(storyboardId.toString()))
                .andExpect(jsonPath("$.data.scheduledAt").isString())
                .andExpect(jsonPath("$.data.createdAt").isString())
                .andDo(document("reservation/interview-get-by-id",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("reservationId").description("조회할 예약 ID")
                        ),
                        responseFields(
                                fieldWithPath("statusCode").description("응답 상태 코드"),
                                fieldWithPath("message").description("응답 상태 메시지"),
                                fieldWithPath("data.id").description("예약 ID"),
                                fieldWithPath("data.memberId").description("예약한 회원 ID"),
                                fieldWithPath("data.storyboardId").description("예약한 스토리보드 ID"),
                                fieldWithPath("data.scheduledAt").description("예약 대상 일시"),
                                fieldWithPath("data.createdAt").description("예약 생성 일시")
                        )
                ));
    }
}