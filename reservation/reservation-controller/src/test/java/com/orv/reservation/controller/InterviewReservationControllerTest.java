package com.orv.reservation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.orv.reservation.orchestrator.InterviewReservationOrchestrator;
import com.orv.reservation.controller.dto.*;
import com.orv.reservation.orchestrator.dto.InterviewReservationResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class InterviewReservationControllerTest {
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Mock
    private InterviewReservationOrchestrator interviewreservationOrchestrator;

    @InjectMocks
    private InterviewReservationController interviewReservationController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(interviewReservationController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("054c3e8a-3387-4eb3-ac8a-31a48221f192", null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
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
                .andExpect(jsonPath("$.data.createdAt").isString());
    }

    @Test
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
                .andExpect(jsonPath("$.data[0].createdAt").isString());
    }

    @Test
    public void testDoneInterview() throws Exception {
        // given
        UUID interviewId = UUID.fromString("e5895e70-7713-4a32-b15f-2521af77524b");
        when(interviewreservationOrchestrator.markInterviewAsDone(any())).thenReturn(true);

        // when
        ResultActions resultActions = mockMvc.perform(patch("/api/v0/reservation/interview/{interviewId}/done", interviewId));

        // then
        resultActions.andExpect(status().isOk());
    }


    @Test
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
                .andExpect(jsonPath("$.data.createdAt").isString());
    }
}
