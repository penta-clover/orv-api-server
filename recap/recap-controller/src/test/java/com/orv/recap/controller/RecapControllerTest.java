package com.orv.recap.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.orv.recap.controller.RecapController;
import com.orv.recap.controller.dto.*;
import com.orv.recap.orchestrator.dto.*;
import com.orv.recap.orchestrator.RecapOrchestrator;
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
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class RecapControllerTest {
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Mock
    private RecapOrchestrator recapOrchestrator;

    @InjectMocks
    private RecapController recapController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(recapController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("054c3e8a-3387-4eb3-ac8a-31a48221f192", null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
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
                .andExpect(jsonPath("$.data.createdAt").isString());
    }


    @Test
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
                .andExpect(jsonPath("$.data.answerSummaries[1].answerSummary").value(summary2.getAnswerSummary()));
    }

    @Test
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
                .andExpect(jsonPath("$.data.createdAt").isString());
    }
}
