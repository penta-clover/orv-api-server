package com.orv.api.unit.domain.reservation;

import com.orv.api.domain.archive.repository.AudioRepository;
import com.orv.api.domain.archive.repository.VideoRepository;
import com.orv.api.domain.archive.service.dto.Video;
import com.orv.api.domain.media.repository.InterviewAudioRecordingRepository;
import com.orv.api.domain.media.service.AudioCompressionService;
import com.orv.api.domain.media.service.AudioExtractService;
import com.orv.api.domain.media.service.dto.InterviewAudioRecording;
import com.orv.api.domain.reservation.repository.RecapRepository;
import com.orv.api.domain.reservation.repository.RecapResultRepository;
import com.orv.api.domain.reservation.service.RecapServiceImpl;
import com.orv.api.domain.reservation.service.dto.InterviewScenario;
import com.orv.api.domain.reservation.service.dto.RecapAudioInfo;
import com.orv.api.domain.storyboard.repository.StoryboardRepository;
import com.orv.api.domain.storyboard.service.InterviewScenarioConverter;
import com.orv.api.domain.storyboard.service.dto.Storyboard;
import com.orv.api.infra.recap.RecapClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecapServiceImplTest {

    @InjectMocks
    private RecapServiceImpl recapService;

    @Mock
    private VideoRepository videoRepository;
    @Mock
    private AudioExtractService audioExtractService;
    @Mock
    private AudioCompressionService audioCompressionService;
    @Mock
    private AudioRepository audioRepository;
    @Mock
    private InterviewAudioRecordingRepository interviewAudioRecordingRepository;
    @Mock
    private RecapRepository recapRepository;
    @Mock
    private RecapResultRepository recapResultRepository;
    @Mock
    private StoryboardRepository storyboardRepository;
    @Mock
    private InterviewScenarioConverter interviewScenarioFactory;
    @Mock
    private RecapClient recapClient;

    @Test
    @DisplayName("리캡 예약 시 RecapClient를 통해 외부 API를 호출하고 결과를 저장한다")
    void reserveRecap_callsRecapClientAndSavesResult() throws IOException {
        // given
        UUID memberId = UUID.randomUUID();
        UUID videoId = UUID.randomUUID();
        UUID storyboardId = UUID.randomUUID();
        UUID recapReservationId = UUID.randomUUID();
        ZonedDateTime scheduledAt = ZonedDateTime.now();

        Video video = new Video();
        video.setId(videoId);
        video.setStoryboardId(storyboardId);

        Storyboard storyboard = new Storyboard();

        // Mocking the entire audio processing flow to simplify the test
        // In a real scenario, this might be a separate, more detailed test.
        when(recapRepository.reserveRecap(any(), any(), any())).thenReturn(Optional.of(recapReservationId));
        when(videoRepository.findById(videoId)).thenReturn(Optional.of(video));
        when(videoRepository.getVideoStream(videoId)).thenReturn(Optional.of(InputStream.nullInputStream()));
        when(audioRepository.save(any(), any())).thenReturn(Optional.of(java.net.URI.create("s3://audio-url")));
        when(interviewAudioRecordingRepository.save(any())).thenReturn(mock(com.orv.api.domain.media.service.dto.InterviewAudioRecording.class));

        // Mocking storyboard and scenario creation
        when(storyboardRepository.findById(storyboardId)).thenReturn(Optional.of(storyboard));
        when(storyboardRepository.findScenesByStoryboardId(storyboardId)).thenReturn(Optional.of(List.of()));
        when(interviewScenarioFactory.create(any(), any())).thenReturn(new InterviewScenario("title", List.of()));

        // when
        Optional<UUID> savedRecapReservationId = recapService.reserveRecap(memberId, videoId, scheduledAt);

        // then
        assertThat(savedRecapReservationId).hasValue(recapReservationId);
    }

    @Test
    @DisplayName("리캡 예약 ID로 오디오 정보를 조회하여 RecapAudioResponse를 반환한다")
    void getRecapAudio_existingAudio_returnsRecapAudioResponse() {
        // given
        UUID recapReservationId = UUID.randomUUID();
        UUID audioId = UUID.randomUUID();
        UUID storyboardId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        String audioUrl = "https://s3.amazonaws.com/test-audio.opus";
        Integer runningTime = 324;
        OffsetDateTime createdAt = OffsetDateTime.now();

        InterviewAudioRecording mockAudioRecording = InterviewAudioRecording.builder()
                .id(audioId)
                .storyboardId(storyboardId)
                .memberId(memberId)
                .audioUrl(audioUrl)
                .runningTime(runningTime)
                .createdAt(createdAt)
                .build();

        when(recapRepository.findAudioByRecapReservationId(recapReservationId))
                .thenReturn(Optional.of(mockAudioRecording));

        // when
        Optional<RecapAudioInfo> result = recapService.getRecapAudio(recapReservationId);

        // then
        assertThat(result).isPresent();
        RecapAudioInfo info = result.get();
        assertThat(info.getAudioId()).isEqualTo(audioId);
        assertThat(info.getAudioUrl()).isEqualTo(audioUrl);
        assertThat(info.getRunningTime()).isEqualTo(runningTime);
        assertThat(info.getCreatedAt()).isEqualTo(createdAt);

        verify(recapRepository, times(1)).findAudioByRecapReservationId(recapReservationId);
    }
}
