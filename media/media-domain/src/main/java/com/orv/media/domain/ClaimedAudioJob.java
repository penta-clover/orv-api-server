package com.orv.media.domain;

import java.time.LocalDateTime;

public record ClaimedAudioJob(AudioExtractionJob job, AudioExtractionJobStatus previousStatus, LocalDateTime previousStartedAt) {
}
