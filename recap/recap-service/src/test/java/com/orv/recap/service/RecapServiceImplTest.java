package com.orv.recap.service;

import com.orv.media.domain.InterviewAudioRecording;
import com.orv.recap.repository.RecapReservationRepository;
import com.orv.recap.repository.RecapResultRepository;
import com.orv.recap.service.RecapServiceImpl;
import com.orv.recap.domain.RecapAudioInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
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
    private RecapReservationRepository recapRepository;
    @Mock
    private RecapResultRepository recapResultRepository;

    @Test
    @DisplayName("리캡 예약 시 Repository를 통해 저장한다")
    void reserveRecap_savesToRepository() {
        // given
        UUID memberId = UUID.randomUUID();
        UUID videoId = UUID.randomUUID();
        UUID recapReservationId = UUID.randomUUID();
        ZonedDateTime scheduledAt = ZonedDateTime.now();

        when(recapRepository.reserveRecap(any(), any(), any())).thenReturn(Optional.of(recapReservationId));

        // when
        Optional<UUID> savedRecapReservationId = recapService.reserveRecap(memberId, videoId, scheduledAt);

        // then
        assertThat(savedRecapReservationId).hasValue(recapReservationId);
        verify(recapRepository, times(1)).reserveRecap(memberId, videoId, scheduledAt.toLocalDateTime());
    }

    @Test
    @DisplayName("리캡 예약 ID로 오디오 정보를 조회하여 RecapAudioResponse를 반환한다")
    void getRecapAudio_existingAudio_returnsRecapAudioResponse() {
        // given
        UUID recapReservationId = UUID.randomUUID();
        UUID audioId = UUID.randomUUID();
        UUID storyboardId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        String audioFileKey = "archive/audios/test-audio-id";
        Integer runningTime = 324;
        OffsetDateTime createdAt = OffsetDateTime.now();

        InterviewAudioRecording mockAudioRecording = InterviewAudioRecording.builder()
                .id(audioId)
                .storyboardId(storyboardId)
                .memberId(memberId)
                .audioFileKey(audioFileKey)
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
        assertThat(info.getAudioFileKey()).isEqualTo(audioFileKey);
        assertThat(info.getRunningTime()).isEqualTo(runningTime);
        assertThat(info.getCreatedAt()).isEqualTo(createdAt);

        verify(recapRepository, times(1)).findAudioByRecapReservationId(recapReservationId);
    }
}
