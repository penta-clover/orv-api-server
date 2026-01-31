package com.orv.archive.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.orv.archive.controller.ArchiveControllerV1;
import com.orv.archive.controller.dto.ConfirmUploadRequest;
import com.orv.archive.orchestrator.dto.PresignedUrlResponse;
import com.orv.archive.orchestrator.ArchiveOrchestrator;

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

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class ArchiveControllerV1Test {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Mock
    private ArchiveOrchestrator archiveOrchestrator;

    @InjectMocks
    private ArchiveControllerV1 archiveControllerV1;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(archiveControllerV1)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("054c3e8a-3387-4eb3-ac8a-31a48221f192", null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void getUploadUrl_success() throws Exception {
        // given
        String storyboardId = "3bc32ef3-2dfc-27a9-b9be-f2bec52efdf3";
        String videoId = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";
        String uploadUrl = "https://orv-bucket.s3.ap-northeast-2.amazonaws.com/archive/videos/" + videoId + "?X-Amz-Algorithm=...";
        Instant expiresAt = Instant.now().plusSeconds(3600);

        PresignedUrlResponse response = new PresignedUrlResponse(videoId, uploadUrl, expiresAt);
        when(archiveOrchestrator.requestUploadUrl(any(UUID.class), any(UUID.class))).thenReturn(response);

        // when
        ResultActions result = mockMvc.perform(get("/api/v1/archive/upload-url")
                .param("storyboardId", storyboardId));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.videoId").value(videoId))
                .andExpect(jsonPath("$.data.uploadUrl").value(uploadUrl))
                .andExpect(jsonPath("$.data.expiresAt").exists());
    }

    @Test
    void getUploadUrl_invalidStoryboardId() throws Exception {
        // when
        ResultActions result = mockMvc.perform(get("/api/v1/archive/upload-url")
                .param("storyboardId", "invalid-uuid"));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("400"))
                .andExpect(jsonPath("$.message").value("fail"));
    }

    @Test
    void confirmRecordedVideo_success() throws Exception {
        // given
        String videoId = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";
        ConfirmUploadRequest request = new ConfirmUploadRequest(videoId);

        when(archiveOrchestrator.confirmUpload(any(UUID.class), any(UUID.class)))
                .thenReturn(Optional.of(videoId));

        // when
        ResultActions result = mockMvc.perform(post("/api/v1/archive/recorded-video")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(videoId));
    }

    @Test
    void confirmRecordedVideo_videoNotFound() throws Exception {
        // given
        String videoId = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";
        ConfirmUploadRequest request = new ConfirmUploadRequest(videoId);

        when(archiveOrchestrator.confirmUpload(any(UUID.class), any(UUID.class)))
                .thenReturn(Optional.empty());

        // when
        ResultActions result = mockMvc.perform(post("/api/v1/archive/recorded-video")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("404"))
                .andExpect(jsonPath("$.message").value("fail"));
    }

    @Test
    void confirmRecordedVideo_invalidVideoId() throws Exception {
        // given
        ConfirmUploadRequest request = new ConfirmUploadRequest("invalid-uuid");

        // when
        ResultActions result = mockMvc.perform(post("/api/v1/archive/recorded-video")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("400"))
                .andExpect(jsonPath("$.message").value("fail"));
    }
}
