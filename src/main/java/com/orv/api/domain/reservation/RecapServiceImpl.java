package com.orv.api.domain.reservation;

import com.orv.api.domain.archive.AudioRepository;
import com.orv.api.domain.archive.VideoRepository;
import com.orv.api.domain.archive.dto.AudioMetadata;
import com.orv.api.domain.archive.dto.Video;
import com.orv.api.domain.media.AudioCompressionService;
import com.orv.api.domain.media.AudioExtractService;
import com.orv.api.domain.media.dto.InterviewAudioRecording;
import com.orv.api.domain.media.repository.InterviewAudioRecordingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecapServiceImpl implements RecapService {

    private final VideoRepository videoRepository;
    private final AudioExtractService audioExtractService;
    private final AudioCompressionService audioCompressionService;
    private final AudioRepository audioRepository;
    private final InterviewAudioRecordingRepository interviewAudioRecordingRepository;
    private final RecapRepository recapRepository;

    @Override
    public Optional<UUID> reserveRecap(UUID memberId, UUID videoId, ZonedDateTime scheduledAt) throws IOException {
        // 1. DB에 리캡 예약 정보 저장
        Optional<UUID> recapReservationIdOptional = recapRepository.reserveRecap(memberId, videoId, scheduledAt.toLocalDateTime());
        if (recapReservationIdOptional.isEmpty()) {
            log.error("Failed to reserve recap for video ID: {}", videoId);
            return Optional.empty();
        }
        UUID recapReservationId = recapReservationIdOptional.get();
        log.info("Recap reservation saved to DB with ID: {}", recapReservationId);

        // 2. 오디오 처리 및 저장
        processRecap(videoId, memberId, recapReservationId);

        return recapReservationIdOptional;
    }

    private void processRecap(UUID videoId, UUID memberId, UUID recapReservationId) throws IOException {
        Optional<Video> videoOptional = videoRepository.findById(videoId);
        if (videoOptional.isEmpty()) {
            log.error("Video with ID {} not found for recap processing.", videoId);
            throw new IOException("Video not found.");
        }
        Video video = videoOptional.get();

        Optional<InputStream> videoStreamOptional = videoRepository.getVideoStream(videoId);
        if (videoStreamOptional.isEmpty()) {
            log.error("Failed to get video stream for video ID {}.", videoId);
            throw new IOException("Failed to retrieve video stream.");
        }

        Path tempVideoPath = null;
        Path tempAudioExtractedPath = null;
        Path tempAudioCompressedPath = null;
        File tempVideoFile = null;
        File tempAudioExtractedFile = null;
        File tempAudioCompressedFile = null;

        try (InputStream videoInputStream = videoStreamOptional.get()) {
            // 1. Download video stream to a temporary file
            tempVideoPath = Files.createTempFile("recap_video_", ".mp4");
            tempVideoFile = tempVideoPath.toFile();
            Files.copy(videoInputStream, tempVideoPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            log.info("Video downloaded to temporary file: {}", tempVideoFile.getAbsolutePath());

            // 2. Extract audio (Video -> WAV)
            tempAudioExtractedPath = Files.createTempFile("extracted_audio_", ".wav");
            tempAudioExtractedFile = tempAudioExtractedPath.toFile();
            log.info("Extracting audio from video: {}", tempVideoFile.getAbsolutePath());
            audioExtractService.extractAudio(tempVideoFile, tempAudioExtractedFile, "wav");
            log.info("Audio extracted to: {}", tempAudioExtractedFile.getAbsolutePath());

            // 3. Compress audio (WAV -> Opus)
            tempAudioCompressedPath = Files.createTempFile("compressed_audio_", ".opus");
            tempAudioCompressedFile = tempAudioCompressedPath.toFile();
            log.info("Compressing audio: {}", tempAudioExtractedFile.getAbsolutePath());
            audioCompressionService.compress(tempAudioExtractedFile, tempAudioCompressedFile);
            log.info("Audio compressed to: {}", tempAudioCompressedFile.getAbsolutePath());

            // 4. Upload compressed audio to S3
            URI resourceUrl;
            try (InputStream compressedAudioInputStream = new FileInputStream(tempAudioCompressedFile)) {
                AudioMetadata audioMetadata = new AudioMetadata(
                        video.getStoryboardId(),
                        memberId,
                        video.getTitle() != null ? video.getTitle() + " (Recap Audio)" : "Recap Audio",
                        "audio/opus",
                        video.getRunningTime(), // Reusing video's running time for now
                        tempAudioCompressedFile.length()
                );
                resourceUrl = audioRepository.save(compressedAudioInputStream, audioMetadata)
                        .orElseThrow(() -> new IOException("Failed to upload compressed audio to S3."));
            }
            log.info("Compressed audio uploaded to S3: {}", resourceUrl);

            // 5. Save recap audio metadata to DB
            InterviewAudioRecording recapAudioRecording = InterviewAudioRecording.builder()
                    .id(UUID.randomUUID())
                    .storyboardId(video.getStoryboardId())
                    .memberId(memberId)
                    .audioUrl(resourceUrl.toString()) // Storing S3 audio URL in audioUrl field
                    .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                    .runningTime(video.getRunningTime())
                    .build();
            InterviewAudioRecording savedAudioRecording = interviewAudioRecordingRepository.save(recapAudioRecording);

            if (savedAudioRecording != null) {
                UUID audioRecordingId = savedAudioRecording.getId();
                recapRepository.linkAudioRecording(recapReservationId, audioRecordingId);
                log.info("Recap audio metadata saved and linked to reservation ID: {}", recapReservationId);
            } else {
                log.error("Failed to save recap audio metadata to DB for video ID: {}", videoId);
            }

        } catch (Exception e) {
            log.error("Error processing recap for video ID {}: {}", videoId, e.getMessage(), e);
            throw new IOException("Recap processing failed for video ID: " + videoId, e);
        } finally {
            // Clean up temporary files
            if (tempVideoFile != null && tempVideoFile.exists()) {
                Files.deleteIfExists(tempVideoPath);
                log.info("Deleted temporary video file: {}", tempVideoFile.getAbsolutePath());
            }
            if (tempAudioExtractedFile != null && tempAudioExtractedFile.exists()) {
                Files.deleteIfExists(tempAudioExtractedPath);
                log.info("Deleted temporary extracted audio file: {}", tempAudioExtractedFile.getAbsolutePath());
            }
            if (tempAudioCompressedFile != null && tempAudioCompressedFile.exists()) {
                Files.deleteIfExists(tempAudioCompressedPath);
                log.info("Deleted temporary compressed audio file: {}", tempAudioCompressedFile.getAbsolutePath());
            }
        }
    }
}
