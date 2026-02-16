package com.orv.archive.service;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.orv.archive.common.ArchiveErrorCode;
import com.orv.archive.common.ArchiveException;
import com.orv.archive.domain.PresignedUrlInfo;
import com.orv.archive.domain.Video;
import com.orv.archive.domain.VideoStatus;
import com.orv.archive.repository.VideoDurationCalculationJobRepository;
import com.orv.archive.repository.VideoRepository;
import com.orv.archive.repository.VideoThumbnailExtractionJobRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArchiveServiceImplTest {

    @InjectMocks
    private ArchiveServiceImpl archiveService;

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private VideoDurationCalculationJobRepository videoDurationCalculationJobRepository;

    @Mock
    private VideoThumbnailExtractionJobRepository videoThumbnailExtractionJobRepository;

    @Test
    @DisplayName("requestUploadUrl: PENDING 상태의 video 생성 후 Presigned URL 반환")
    void requestUploadUrl_createsPendingVideoAndReturnsPresignedUrl() throws MalformedURLException {
        // given
        UUID storyboardId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        String videoId = UUID.randomUUID().toString();
        URL presignedUrl = new URL("https://bucket.s3.amazonaws.com/archive/videos/" + videoId + "?X-Amz-Signature=...");

        when(videoRepository.createPendingVideo(storyboardId, memberId)).thenReturn(videoId);
        when(videoRepository.generateUploadUrl(eq(UUID.fromString(videoId)), eq(60L))).thenReturn(presignedUrl);

        // when
        PresignedUrlInfo response = archiveService.requestUploadUrl(storyboardId, memberId);

        // then
        assertThat(response.getVideoId()).isEqualTo(videoId);
        assertThat(response.getUploadUrl()).isEqualTo(presignedUrl.toString());
        assertThat(response.getExpiresAt()).isNotNull();
    }

    @Test
    @DisplayName("confirmUpload: S3에 파일 존재 시 status를 UPLOADED로 변경")
    void confirmUpload_returnsVideoIdWhenSuccessful() {
        // given
        UUID videoId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        String cloudfrontDomain = "https://cdn.example.com";

        Video video = new Video();
        video.setId(videoId);
        video.setMemberId(memberId);
        video.setStatus(VideoStatus.PENDING.name());

        ReflectionTestUtils.setField(archiveService, "cloudfrontDomain", cloudfrontDomain);

        when(videoRepository.findById(videoId)).thenReturn(Optional.of(video));
        when(videoRepository.checkUploadComplete(videoId)).thenReturn(true);
        when(videoRepository.updateVideoFileKeyAndStatus(eq(videoId), any(), eq(VideoStatus.UPLOADED.name()))).thenReturn(true);

        // when
        String result = archiveService.confirmUpload(videoId, memberId);

        // then
        assertThat(result).isEqualTo(videoId.toString());
    }

    @Test
    @DisplayName("confirmUpload: video가 존재하지 않으면 예외 발생")
    void confirmUpload_throwsExceptionWhenVideoNotFound() {
        // given
        UUID videoId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();

        when(videoRepository.findById(videoId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> archiveService.confirmUpload(videoId, memberId))
                .isInstanceOf(ArchiveException.class)
                .hasFieldOrPropertyWithValue("errorCode", ArchiveErrorCode.VIDEO_NOT_FOUND);
    }

    @Test
    @DisplayName("confirmUpload: 다른 사용자의 video면 예외 발생")
    void confirmUpload_throwsExceptionWhenUnauthorized() {
        // given
        UUID videoId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        UUID differentMemberId = UUID.randomUUID();

        Video video = new Video();
        video.setId(videoId);
        video.setMemberId(differentMemberId);
        video.setStatus(VideoStatus.PENDING.name());

        when(videoRepository.findById(videoId)).thenReturn(Optional.of(video));

        // when & then
        assertThatThrownBy(() -> archiveService.confirmUpload(videoId, memberId))
                .isInstanceOf(ArchiveException.class)
                .hasFieldOrPropertyWithValue("errorCode", ArchiveErrorCode.VIDEO_ACCESS_DENIED);
    }

    @Test
    @DisplayName("confirmUpload: PENDING 상태가 아니면 예외 발생")
    void confirmUpload_throwsExceptionWhenNotPending() {
        // given
        UUID videoId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();

        Video video = new Video();
        video.setId(videoId);
        video.setMemberId(memberId);
        video.setStatus(VideoStatus.UPLOADED.name());

        when(videoRepository.findById(videoId)).thenReturn(Optional.of(video));

        // when & then
        assertThatThrownBy(() -> archiveService.confirmUpload(videoId, memberId))
                .isInstanceOf(ArchiveException.class)
                .hasFieldOrPropertyWithValue("errorCode", ArchiveErrorCode.VIDEO_STATUS_NOT_PENDING);
    }

    @Test
    @DisplayName("confirmUpload: S3에 파일이 없으면 예외 발생")
    void confirmUpload_throwsExceptionWhenFileNotInS3() {
        // given
        UUID videoId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();

        Video video = new Video();
        video.setId(videoId);
        video.setMemberId(memberId);
        video.setStatus(VideoStatus.PENDING.name());

        when(videoRepository.findById(videoId)).thenReturn(Optional.of(video));
        when(videoRepository.checkUploadComplete(videoId)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> archiveService.confirmUpload(videoId, memberId))
                .isInstanceOf(ArchiveException.class)
                .hasFieldOrPropertyWithValue("errorCode", ArchiveErrorCode.VIDEO_FILE_NOT_UPLOADED);
    }

    @Test
    @DisplayName("confirmUpload: DB 업데이트 실패 시 예외 발생")
    void confirmUpload_throwsExceptionWhenStatusUpdateFails() {
        // given
        UUID videoId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        String cloudfrontDomain = "https://cdn.example.com";

        Video video = new Video();
        video.setId(videoId);
        video.setMemberId(memberId);
        video.setStatus(VideoStatus.PENDING.name());

        ReflectionTestUtils.setField(archiveService, "cloudfrontDomain", cloudfrontDomain);

        when(videoRepository.findById(videoId)).thenReturn(Optional.of(video));
        when(videoRepository.checkUploadComplete(videoId)).thenReturn(true);
        when(videoRepository.updateVideoFileKeyAndStatus(eq(videoId), any(), eq(VideoStatus.UPLOADED.name()))).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> archiveService.confirmUpload(videoId, memberId))
                .isInstanceOf(ArchiveException.class)
                .hasFieldOrPropertyWithValue("errorCode", ArchiveErrorCode.VIDEO_STATUS_UPDATE_FAILED);
    }
}
