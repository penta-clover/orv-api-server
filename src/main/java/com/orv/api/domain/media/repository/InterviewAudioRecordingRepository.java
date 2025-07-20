package com.orv.api.domain.media.repository;

import com.orv.api.domain.media.dto.InterviewAudioRecording;

import java.util.Optional;
import java.util.UUID;

public interface InterviewAudioRecordingRepository {
    InterviewAudioRecording save(InterviewAudioRecording audioRecording);
    Optional<InterviewAudioRecording> findById(UUID id);
}
