package com.orv.api.domain.reservation.service;

import com.orv.api.domain.reservation.repository.RecapRepository;
import com.orv.api.domain.reservation.repository.RecapResultRepository;
import com.orv.api.domain.reservation.service.dto.RecapAnswerSummaryInfo;
import com.orv.api.domain.reservation.service.dto.RecapAudioInfo;
import com.orv.api.domain.reservation.service.dto.RecapContent;
import com.orv.api.domain.reservation.service.dto.RecapResultInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecapServiceImpl implements RecapService {

    private final RecapRepository recapRepository;
    private final RecapResultRepository recapResultRepository;

    @Override
    public Optional<UUID> reserveRecap(UUID memberId, UUID videoId, ZonedDateTime scheduledAt) {
        // 1. DB에 리캡 예약 정보 저장
        Optional<UUID> recapReservationIdOptional = recapRepository.reserveRecap(memberId, videoId, scheduledAt.toLocalDateTime());
        if (recapReservationIdOptional.isEmpty()) {
            log.error("Failed to reserve recap for video ID: {}", videoId);
            return Optional.empty();
        }
        UUID recapReservationId = recapReservationIdOptional.get();
        log.info("Recap reservation saved to DB with ID: {}", recapReservationId);

        return recapReservationIdOptional;
    }

    @Override
    public void linkAudioRecording(UUID recapReservationId, UUID audioRecordingId) {
        recapRepository.linkAudioRecording(recapReservationId, audioRecordingId);
    }

    @Override
    public Optional<UUID> saveRecapResult(UUID recapReservationId, List<RecapContent> recapContent) {
        return recapResultRepository.save(recapReservationId, recapContent);
    }

    @Override
    public Optional<RecapResultInfo> getRecapResult(UUID recapReservationId) {
        return recapResultRepository.findByRecapReservationId(recapReservationId)
                .map(response -> new RecapResultInfo(
                        response.getRecapResultId(),
                        response.getCreatedAt(),
                        response.getAnswerSummaries().stream()
                                .map(summary -> new RecapAnswerSummaryInfo(
                                        summary.getSceneId(),
                                        summary.getQuestion(),
                                        summary.getAnswerSummary()
                                ))
                                .collect(Collectors.toList())
                ));
    }

    @Override
    public Optional<RecapAudioInfo> getRecapAudio(UUID recapReservationId) {
        return recapRepository.findAudioByRecapReservationId(recapReservationId)
                .map(audioRecording -> new RecapAudioInfo(
                        audioRecording.getId(),
                        audioRecording.getAudioUrl(),
                        audioRecording.getRunningTime(),
                        audioRecording.getCreatedAt()
                ));
    }
}