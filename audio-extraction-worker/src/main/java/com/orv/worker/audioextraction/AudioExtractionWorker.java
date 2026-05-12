package com.orv.worker.audioextraction;

import java.time.Duration;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import com.orv.media.domain.AudioExtractionJob;
import com.orv.media.domain.ClaimedAudioJob;
import com.orv.media.repository.AudioExtractionJobRepository;
import com.orv.worker.audioextraction.service.AudioExtractionJobService;

import lombok.extern.slf4j.Slf4j;

@Component
@EnableScheduling
@Slf4j
public class AudioExtractionWorker {

    private final AudioExtractionJobRepository jobRepository;
    private final AudioExtractionJobService jobService;
    private final ThreadPoolTaskExecutor executor;
    private final Duration stuckThreshold;

    public AudioExtractionWorker(
            AudioExtractionJobRepository jobRepository,
            AudioExtractionJobService jobService,
            @Qualifier("audioExtractionExecutor") ThreadPoolTaskExecutor executor,
            @Value("${worker.audio-extraction.stuck-threshold-minutes:15}") int stuckThresholdMinutes) {
        this.jobRepository = jobRepository;
        this.jobService = jobService;
        this.executor = executor;
        this.stuckThreshold = Duration.ofMinutes(stuckThresholdMinutes);
    }

    @Scheduled(fixedDelayString = "${worker.audio-extraction.poll-interval-ms:1000}")
    public void poll() {
        while (hasAvailableThread()) {
            Optional<ClaimedAudioJob> claimedOpt = jobRepository.claimNext(stuckThreshold);

            if (claimedOpt.isEmpty()) {
                return;
            }

            ClaimedAudioJob claimed = claimedOpt.get();
            AudioExtractionJob job = claimed.job();
            log.info("Dispatching audio extraction job #{} for video {} (claimed as {})",
                    job.getId(), job.getVideoId(), job.getStatus());

            try {
                executor.execute(() -> jobService.processJob(job));
            } catch (org.springframework.core.task.TaskRejectedException e) {
                log.warn("Thread pool full, restoring audio extraction job #{} to {}",
                        job.getId(), claimed.previousStatus());
                jobRepository.resetToPreClaimState(job.getId(), claimed.previousStatus(), claimed.previousStartedAt());
                return;
            }
        }
    }

    private boolean hasAvailableThread() {
        return executor.getActiveCount() < executor.getMaxPoolSize();
    }
}
