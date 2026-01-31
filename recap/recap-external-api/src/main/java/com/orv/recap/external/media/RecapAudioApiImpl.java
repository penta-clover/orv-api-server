package com.orv.recap.external.media;

import com.orv.media.service.AudioService;
import com.orv.media.domain.InterviewAudioRecording;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RecapAudioApiImpl implements RecapAudioApi {
    private final AudioService audioService;

    @Override
    public AudioRecordingInfo extractAndSaveAudioFromVideo(
        InputStream videoStream,
        UUID storyboardId,
        UUID memberId,
        String title,
        Integer runningTime
    ) throws IOException {
        InterviewAudioRecording recording = audioService.extractAndSaveAudioFromVideo(
            videoStream, storyboardId, memberId, title, runningTime
        );

        return new AudioRecordingInfo(recording.getId(), recording.getAudioUrl());
    }
}
