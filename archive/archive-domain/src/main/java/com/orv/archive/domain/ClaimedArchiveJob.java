package com.orv.archive.domain;

import java.time.LocalDateTime;

public record ClaimedArchiveJob<T>(T job, JobStatus previousStatus, LocalDateTime previousStartedAt) {
}
