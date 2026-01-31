package com.orv.media.repository;

import java.util.Optional;
import java.util.UUID;

import com.orv.media.domain.InterviewAudioRecording;

public interface InterviewAudioRecordingRepository {
    InterviewAudioRecording save(InterviewAudioRecording audioRecording);
    Optional<InterviewAudioRecording> findById(UUID id);
    void delete(UUID id);
}
