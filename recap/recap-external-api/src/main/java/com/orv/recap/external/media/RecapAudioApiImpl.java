package com.orv.recap.external.media;

import com.orv.media.service.AudioService;
import com.orv.media.domain.InterviewAudioRecording;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RecapAudioApiImpl implements RecapAudioApi {
    private final AudioService audioService;

    @Override
    public AudioRecordingInfo extractAndSaveAudioFromVideo(
        File videoFile,
        UUID storyboardId,
        UUID memberId
    ) throws IOException {
        InterviewAudioRecording recording = audioService.extractAndSaveAudioFromVideo(
            videoFile, storyboardId, memberId
        );

        return new AudioRecordingInfo(recording.getId(), recording.getAudioUrl());
    }
}
