package com.orv.api.unit.domain.archive;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.orv.api.domain.archive.ArchiveController;
import com.orv.api.domain.archive.VideoRepository;
import com.orv.api.domain.archive.dto.Video;
import com.orv.api.domain.archive.dto.VideoMetadataUpdateForm;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.payload.PayloadDocumentation;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@WebMvcTest(ArchiveController.class)
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@AutoConfigureMockMvc(addFilters = false)
public class ArchiveControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VideoRepository videoRepository;

    @Test
    @WithMockUser(username = "054c3e8a-3387-4eb3-ac8a-31a48221f192")
    public void testUploadRecordedVideo() throws Exception {
        // given
        InputStream videoStream = getClass().getResourceAsStream("/videos/upload-test-video.mp4");
        assertNotNull(videoStream, "테스트 비디오 파일을 찾을 수 없습니다.");
        MockMultipartFile video = new MockMultipartFile("video", "test.mp4", "video/mp4", videoStream);

        // storyboardId를 MultipartFile로 생성
        String storyboardIdValue = "3bc32ef3-2dfc-27a9-b9be-f2bec52efdf3";

        String videoId = "3bc32ef3-2dfc-27a9-b9be-f2bec52efdf3";
        when(videoRepository.save(any(), any())).thenReturn(Optional.of(videoId));

        // when
        ResultActions resultActions = mockMvc.perform(multipart("/api/v0/archive/recorded-video")
                .file(video)
                .param("storyboardId", storyboardIdValue));

        // then
        resultActions
                .andExpect(jsonPath("$.data").value(videoId))
                .andDo(document("archive/upload-recorded-video",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestParts(
                                partWithName("video").description("업로드할 비디오 파일 (예: MP4 포맷)")
                        ),
                        responseFields(
                                fieldWithPath("statusCode").description("응답 상태 코드"),
                                fieldWithPath("message").description("응답 상태 메시지"),
                                fieldWithPath("data").description("업로드된 비디오의 ID")
                        )
                ));
    }


    @Test
    @WithMockUser(username = "054c3e8a-3387-4eb3-ac8a-31a48221f192")
    public void testGetStoryboard_whenStoryboardExists() throws Exception {
        // given
        Video video = new Video();
        video.setId(UUID.fromString("24c4dfc2-8bec-4d77-849f-57462d50d36e"));
        video.setStoryboardId(UUID.fromString("e5895e70-7713-4a35-b12f-2521af77524b"));
        video.setMemberId(UUID.fromString("1fae8d62-fdfb-47b2-a91d-182bec52ef47"));
        video.setTitle("video title");
        video.setVideoUrl("https://api.orv.im/test-video.url.mp4");
        video.setCreatedAt(LocalDateTime.now());
        video.setRunningTime(523);
        video.setThumbnailUrl("https://api.orv.im/test-thumbnail.url.jpg");

        when(videoRepository.findById(video.getId())).thenReturn(Optional.of(video));

        // when
        ResultActions resultActions = mockMvc.perform(get("/api/v0/archive/video/{videoId}", video.getId()));

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(video.getId().toString()))
                .andExpect(jsonPath("$.data.storyboardId").value(video.getStoryboardId().toString()))
                .andExpect(jsonPath("$.data.memberId").value(video.getMemberId().toString()))
                .andExpect(jsonPath("$.data.title").value(video.getTitle()))
                .andExpect(jsonPath("$.data.videoUrl").value(video.getVideoUrl()))
                .andExpect(jsonPath("$.data.thumbnailUrl").value(video.getThumbnailUrl()))
                .andExpect(jsonPath("$.data.runningTime").value(video.getRunningTime()))
                .andExpect(jsonPath("$.data.title").value(video.getTitle()))
                .andDo(document("archive/get-video-by-id",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("videoId").description("video의 ID")
                        ),
                        responseFields(
                                fieldWithPath("statusCode").description("응답 상태 코드"),
                                fieldWithPath("message").description("응답 상태 메시지"),
                                fieldWithPath("data.id").description("비디오의 ID"),
                                fieldWithPath("data.storyboardId").description("비디오가 속한 스토리보드의 ID"),
                                fieldWithPath("data.memberId").description("비디오를 업로드한 회원의 ID"),
                                fieldWithPath("data.videoUrl").description("비디오의 URL"),
                                fieldWithPath("data.createdAt").description("비디오의 생성 시각"),
                                fieldWithPath("data.thumbnailUrl").description("비디오의 썸네일 URL"),
                                fieldWithPath("data.runningTime").description("비디오의 재생 시간(초)"),
                                fieldWithPath("data.title").description("비디오의 제목 (default: null)")
                        )
                ));
    }

    @Test
    @WithMockUser(username = "054c3e8a-3387-4eb3-ac8a-31a48221f192")
    public void testChangeVideoMetadata() throws Exception {
        // given
        String videoId = "3bc32ef3-2dfc-27a9-b9be-f2bec52efdf3";
        String title = "test title";
        VideoMetadataUpdateForm updateForm = new VideoMetadataUpdateForm(title);

        when(videoRepository.updateTitle(UUID.fromString(videoId), title)).thenReturn(true);

        // when
        ResultActions resultActions = mockMvc.perform(patch("/api/v0/archive/video/{videoId}", videoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(updateForm)));

        // then
        resultActions
                .andExpect(jsonPath("$.data").doesNotExist())
                .andDo(document("archive/change-video-metadata",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("videoId").description("수정할 비디오의 ID")
                        ),
                        requestFields(
                                fieldWithPath("title").description("수정할 비디오 제목")
                        ),
                        responseFields(
                                fieldWithPath("statusCode").description("응답 상태 코드 (optional)"),
                                fieldWithPath("message").description("응답 상태 메시지"),
                                fieldWithPath("data").description("null")
                        )
                ));
    }

    @Test
    @WithMockUser(username = "054c3e8a-3387-4eb3-ac8a-31a48221f192")
    public void testUpdateThumbnail() throws Exception {
        // given
        String videoId = "3bc32ef3-2dfc-27a9-b9be-f2bec52efdf3";
        InputStream thumbnailStream = getClass().getResourceAsStream("/images/test-thumbnail.png");
        assertNotNull(thumbnailStream, "테스트 썸네일 파일을 찾을 수 없습니다.");

        MockMultipartFile thumbnail = new MockMultipartFile("thumbnail", "test-thumbnail.png", "image/png", thumbnailStream);

        when(videoRepository.updateThumbnail(any(), any(), any())).thenReturn(true);

        // when
        ResultActions resultActions = mockMvc.perform(multipart(HttpMethod.PUT, "/api/v0/archive/video/{videoId}/thumbnail", videoId)
                .file(thumbnail));

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist())
                .andDo(document("archive/update-thumbnail",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("videoId").description("썸네일을 변경할 비디오의 ID")
                        ),
                        requestParts(
                                partWithName("thumbnail").description("변경할 썸네일 이미지 파일")
                        ),
                        responseFields(
                                fieldWithPath("statusCode").description("응답 상태 코드 (optional)"),
                                fieldWithPath("message").description("응답 상태 메시지"),
                                fieldWithPath("data").description("null")
                        )
                ));
    }


    @Test
    @WithMockUser(username = "054c3e8a-3387-4eb3-ac8a-31a48221f192")
    public void testGetMyVideos() throws Exception {
        // given
        Video video1 = new Video();
        video1.setId(UUID.fromString("24c4dfc2-8bec-4d77-849f-57462d50d36e"));
        video1.setStoryboardId(UUID.fromString("e5895e70-7713-4a35-b12f-2521af77524b"));
        video1.setMemberId(UUID.fromString("1fae8d62-fdfb-47b2-a91d-182bec52ef47"));
        video1.setTitle("video title");
        video1.setVideoUrl("https://api.orv.im/test-video.url.mp4");
        video1.setCreatedAt(LocalDateTime.now());
        video1.setRunningTime(523);
        video1.setThumbnailUrl("https://api.orv.im/test-thumbnail.url.jpg");


        Video video2 = new Video();
        video2.setId(UUID.fromString("24c4dfc2-8bec-4d77-849f-57462d50d36e"));
        video2.setStoryboardId(UUID.fromString("e5895e70-7713-4a35-b12f-2521af77524b"));
        video2.setMemberId(UUID.fromString("1fae8d62-fdfb-47b2-a91d-182bec52ef47"));
        video2.setTitle("video title");
        video2.setVideoUrl("https://api.orv.im/test-video.url.mp4");
        video2.setCreatedAt(LocalDateTime.now());
        video2.setRunningTime(523);
        video2.setThumbnailUrl("https://api.orv.im/test-thumbnail.url.jpg");

        when(videoRepository.findByMemberId(any(), anyInt(), anyInt())).thenReturn(List.of(video1, video2));

        // when
        ResultActions resultActions = mockMvc.perform(get("/api/v0/archive/videos/my"));

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(video1.getId().toString()))
                .andExpect(jsonPath("$.data[0].storyboardId").value(video1.getStoryboardId().toString()))
                .andExpect(jsonPath("$.data[0].memberId").value(video1.getMemberId().toString()))
                .andExpect(jsonPath("$.data[0].title").value(video1.getTitle()))
                .andExpect(jsonPath("$.data[0].videoUrl").value(video1.getVideoUrl()))
                .andExpect(jsonPath("$.data[0].thumbnailUrl").value(video1.getThumbnailUrl()))
                .andExpect(jsonPath("$.data[0].runningTime").value(video1.getRunningTime()))
                .andExpect(jsonPath("$.data[0].title").value(video1.getTitle())
                )
                .andExpect(jsonPath("$.data[1].id").value(video2.getId().toString()))
                .andExpect(jsonPath("$.data[1].storyboardId").value(video2.getStoryboardId().toString()))
                .andExpect(jsonPath("$.data[1].memberId").value(video2.getMemberId().toString()))
                .andExpect(jsonPath("$.data[1].title").value(video2.getTitle()))
                .andExpect(jsonPath("$.data[1].videoUrl").value(video2.getVideoUrl()))
                .andExpect(jsonPath("$.data[1].thumbnailUrl").value(video2.getThumbnailUrl()))
                .andExpect(jsonPath("$.data[1].runningTime").value(video2.getRunningTime()))
                .andExpect(jsonPath("$.data[1].title").value(video2.getTitle())
                )
                .andDo(document("archive/get-my-videos",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("statusCode").description("응답 상태 코드"),
                                fieldWithPath("message").description("응답 상태 메시지"),
                                fieldWithPath("data[].id").description("비디오의 ID"),
                                fieldWithPath("data[].storyboardId").description("비디오가 속한 스토리보드의 ID"),
                                fieldWithPath("data[].memberId").description("비디오를 업로드한 회원의 ID"),
                                fieldWithPath("data[].videoUrl").description("비디오의 URL"),
                                fieldWithPath("data[].createdAt").description("비디오의 생성 시각"),
                                fieldWithPath("data[].thumbnailUrl").description("비디오의 썸네일 URL"),
                                fieldWithPath("data[].runningTime").description("비디오의 재생 시간(초)"),
                                fieldWithPath("data[].title").description("비디오의 제목 (default: null)")
                        )
                ));

    }
}
