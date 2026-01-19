package com.orv.api.unit.domain.archive;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orv.api.domain.archive.controller.ArchiveControllerV1;
import com.orv.api.domain.archive.controller.dto.ConfirmUploadRequest;
import com.orv.api.domain.archive.service.ArchiveService;
import com.orv.api.domain.archive.service.dto.PresignedUrlInfo;

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

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ArchiveControllerV1.class)
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@AutoConfigureMockMvc(addFilters = false)
public class ArchiveControllerV1Test {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ArchiveService archiveService;

    @Test
    @WithMockUser(username = "054c3e8a-3387-4eb3-ac8a-31a48221f192")
    void getUploadUrl_success() throws Exception {
        // given
        String storyboardId = "3bc32ef3-2dfc-27a9-b9be-f2bec52efdf3";
        String videoId = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";
        String uploadUrl = "https://orv-bucket.s3.ap-northeast-2.amazonaws.com/archive/videos/" + videoId + "?X-Amz-Algorithm=...";
        Instant expiresAt = Instant.now().plusSeconds(3600);

        PresignedUrlInfo response = new PresignedUrlInfo(videoId, uploadUrl, expiresAt);
        when(archiveService.requestUploadUrl(any(UUID.class), any(UUID.class))).thenReturn(response);

        // when
        ResultActions result = mockMvc.perform(get("/api/v1/archive/upload-url")
                .param("storyboardId", storyboardId));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.videoId").value(videoId))
                .andExpect(jsonPath("$.data.uploadUrl").value(uploadUrl))
                .andExpect(jsonPath("$.data.expiresAt").exists())
                .andDo(document("archive-v1/get-upload-url",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        queryParameters(
                                parameterWithName("storyboardId").description("녹화할 스토리보드의 ID")
                        ),
                        responseFields(
                                fieldWithPath("statusCode").description("응답 상태 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data.videoId").description("생성된 video 레코드의 ID (S3 업로드 시 사용)"),
                                fieldWithPath("data.uploadUrl").description("S3 Presigned PUT URL (클라이언트가 직접 업로드)"),
                                fieldWithPath("data.expiresAt").description("URL 만료 시각 (ISO 8601)")
                        )
                ));
    }

    @Test
    @WithMockUser(username = "054c3e8a-3387-4eb3-ac8a-31a48221f192")
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
    @WithMockUser(username = "054c3e8a-3387-4eb3-ac8a-31a48221f192")
    void confirmRecordedVideo_success() throws Exception {
        // given
        String videoId = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";
        ConfirmUploadRequest request = new ConfirmUploadRequest(videoId);

        when(archiveService.confirmUpload(any(UUID.class), any(UUID.class)))
                .thenReturn(Optional.of(videoId));

        // when
        ResultActions result = mockMvc.perform(post("/api/v1/archive/recorded-video")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(videoId))
                .andDo(document("archive-v1/confirm-recorded-video",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(
                                fieldWithPath("videoId").description("upload-url에서 받은 video ID")
                        ),
                        responseFields(
                                fieldWithPath("statusCode").description("응답 상태 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data").description("확인된 video ID")
                        )
                ));
    }

    @Test
    @WithMockUser(username = "054c3e8a-3387-4eb3-ac8a-31a48221f192")
    void confirmRecordedVideo_videoNotFound() throws Exception {
        // given
        String videoId = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";
        ConfirmUploadRequest request = new ConfirmUploadRequest(videoId);

        when(archiveService.confirmUpload(any(UUID.class), any(UUID.class)))
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
    @WithMockUser(username = "054c3e8a-3387-4eb3-ac8a-31a48221f192")
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
