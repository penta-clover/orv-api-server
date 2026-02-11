package com.orv.archive.repository.s3;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.orv.archive.domain.*;
import com.orv.archive.repository.VideoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import java.io.InputStream;
import java.net.URL;
import java.util.*;

@Repository
@Slf4j
public class S3VideoRepository implements VideoRepository {
    private static final String VIDEO_PATH_PREFIX = "archive/videos/";
    private static final String IMAGE_PATH_PREFIX = "archive/images/";
    private static final String DEFAULT_THUMBNAIL_KEY = "static/images/default-archive-video-thumbnail.png";

    private final AmazonS3 amazonS3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert simpleJdbcInsertVideo;
    private final SimpleJdbcInsert simpleJdbcInsertPendingVideo;

    public S3VideoRepository(AmazonS3 amazonS3Client, JdbcTemplate jdbcTemplate) {
        this.amazonS3Client = amazonS3Client;
        this.jdbcTemplate = jdbcTemplate;
        this.simpleJdbcInsertVideo = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("video")
                .usingColumns("id", "storyboard_id", "member_id", "video_file_key", "thumbnail_file_key", "running_time", "title", "status");
        this.simpleJdbcInsertPendingVideo = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("video")
                .usingColumns("id", "storyboard_id", "member_id", "status");
    }

    public Optional<String> save(InputStream inputStream, VideoMetadata videoMetadata) {
        return save(inputStream, videoMetadata, Optional.empty());
    }

    public Optional<String> save(InputStream inputStream, VideoMetadata videoMetadata, Optional<InputStreamWithMetadata> thumbnailImage) {
        String videoFileId = UUID.randomUUID().toString();

        try {
            String videoKey = uploadVideoToS3(inputStream, videoFileId, videoMetadata);
            String thumbnailKey = uploadThumbnailOrDefault(thumbnailImage);
            return saveVideoRecord(videoFileId, videoMetadata, videoKey, thumbnailKey);
        } catch (Exception e) {
            log.error("Failed to save video: videoId={}, storyboardId={}", videoFileId, videoMetadata.getStoryboardId(), e);
            return Optional.empty();
        }
    }

    private String uploadToS3(InputStream inputStream, String s3Key, String contentType, long contentLength) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);
        metadata.setContentLength(contentLength);
        amazonS3Client.putObject(bucket, s3Key, inputStream, metadata);
        return s3Key;
    }

    private String uploadVideoToS3(InputStream inputStream, String videoFileId, VideoMetadata videoMetadata) {
        String s3Key = VIDEO_PATH_PREFIX + videoFileId;
        return uploadToS3(inputStream, s3Key, videoMetadata.getContentType(), videoMetadata.getContentLength());
    }

    private String uploadThumbnailOrDefault(Optional<InputStreamWithMetadata> thumbnailImage) {
        if (thumbnailImage.isEmpty()) {
            return DEFAULT_THUMBNAIL_KEY;
        }

        InputStreamWithMetadata thumbnail = thumbnailImage.get();
        String thumbnailFileId = UUID.randomUUID().toString();
        String s3Key = IMAGE_PATH_PREFIX + thumbnailFileId;
        return uploadToS3(
                thumbnail.getThumbnailImage(),
                s3Key,
                thumbnail.getMetadata().getContentType(),
                thumbnail.getMetadata().getContentLength()
        );
    }

    private Optional<String> saveVideoRecord(String videoFileId, VideoMetadata videoMetadata, String videoKey, String thumbnailKey) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", videoFileId);
        parameters.put("storyboard_id", videoMetadata.getStoryboardId().toString());
        parameters.put("member_id", videoMetadata.getOwnerId().toString());
        parameters.put("video_file_key", videoKey);
        parameters.put("thumbnail_file_key", thumbnailKey);
        parameters.put("running_time", videoMetadata.getRunningTime());
        parameters.put("title", videoMetadata.getTitle());
        parameters.put("status", VideoStatus.UPLOADED.name());
        simpleJdbcInsertVideo.execute(new MapSqlParameterSource(parameters));
        return Optional.of(videoFileId);
    }

    @Override
    public Optional<Video> findById(UUID videoId) {
        String sql = "SELECT id, storyboard_id, member_id, video_url, video_file_key, created_at, thumbnail_url, thumbnail_file_key, running_time, title, status FROM video WHERE id = ?";

        try {
            Video video = jdbcTemplate.queryForObject(sql, new Object[]{videoId}, new BeanPropertyRowMapper<>(Video.class));
            return Optional.of(video);
        } catch (EmptyResultDataAccessException e) {
            log.debug("Video not found: {}", videoId);
            return Optional.empty();
        }
    }

    @Override
    public List<Video> findByMemberId(UUID memberId, int offset, int limit) {
        String sql = "SELECT id, storyboard_id, member_id, video_url, video_file_key, created_at, thumbnail_url, thumbnail_file_key, running_time, title, status FROM video WHERE member_id = ? AND status = 'UPLOADED' ORDER BY created_at DESC LIMIT ? OFFSET ?";

        try {
            List<Video> videos = jdbcTemplate.query(sql, new Object[]{memberId, limit, offset}, new BeanPropertyRowMapper<>(Video.class));
            return videos;
        } catch (EmptyResultDataAccessException e) {
            log.debug("No videos found for member: {}", memberId);
            return Collections.emptyList();
        }
    }

    @Override
    public boolean updateTitle(UUID videoId, String title) {
        try {
            jdbcTemplate.update("UPDATE video SET title = ? WHERE id = ?", title, videoId);
            return true;
        } catch (Exception e) {
            log.error("Failed to update video title: videoId={}", videoId, e);
            return false;
        }
    }

    @Override
    public boolean updateThumbnail(UUID videoId, String thumbnailFileKey) {
        try {
            int updated = jdbcTemplate.update(
                    "UPDATE video SET thumbnail_file_key = ? WHERE id = ?",
                    thumbnailFileKey, videoId
            );
            return updated > 0;
        } catch (Exception e) {
            log.error("Failed to update thumbnail: videoId={}", videoId, e);
            return false;
        }
    }

    @Override
    public boolean updateThumbnail(UUID videoId, InputStream thumbnail, ImageMetadata imageMetadata) {
        try {
            String fileId = UUID.randomUUID().toString();
            String s3Key = IMAGE_PATH_PREFIX + fileId;
            uploadToS3(thumbnail, s3Key, imageMetadata.getContentType(), imageMetadata.getContentLength());

            jdbcTemplate.update("UPDATE video SET thumbnail_file_key = ? WHERE id = ?", s3Key, videoId);
            return true;
        } catch (Exception e) {
            log.error("Failed to update thumbnail: videoId={}", videoId, e);
            return false;
        }
    }

    @Override
    public Optional<InputStream> getVideoStream(UUID videoId) {
        try {
            String fileKey = jdbcTemplate.queryForObject(
                    "SELECT video_file_key FROM video WHERE id = ?",
                    new Object[]{videoId}, String.class
            );

            if (fileKey == null) {
                return Optional.empty();
            }

            S3Object s3Object = amazonS3Client.getObject(bucket, fileKey);
            return Optional.of(s3Object.getObjectContent());
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to get video stream: videoId={}", videoId, e);
            return Optional.empty();
        }
    }

    // v1 API methods

    @Override
    public String createPendingVideo(UUID storyboardId, UUID memberId) {
        String videoId = UUID.randomUUID().toString();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", videoId);
        parameters.put("storyboard_id", storyboardId.toString());
        parameters.put("member_id", memberId.toString());
        parameters.put("status", VideoStatus.PENDING.name());

        simpleJdbcInsertPendingVideo.execute(new MapSqlParameterSource(parameters));

        return videoId;
    }

    @Override
    public URL generateUploadUrl(UUID videoId, long expirationMinutes) {
        String s3Key = VIDEO_PATH_PREFIX + videoId;
        Date expiration = calcDateAfterMinutes(expirationMinutes);

        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, s3Key)
                .withMethod(HttpMethod.PUT)
                .withExpiration(expiration);

        return amazonS3Client.generatePresignedUrl(request);
    }

    @Override
    public boolean checkUploadComplete(UUID videoId) {
        String s3Key = VIDEO_PATH_PREFIX + videoId;
        try {
            ObjectMetadata metadata = amazonS3Client.getObjectMetadata(bucket, s3Key);
            return metadata != null && metadata.getContentLength() > 0;
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() == 404) {
                return false;
            }
            throw e;
        }
    }

    @Override
    public boolean updateVideoFileKeyAndStatus(UUID videoId, String videoFileKey, String status) {
        try {
            int updated = jdbcTemplate.update(
                    "UPDATE video SET video_file_key = ?, status = ? WHERE id = ?",
                    videoFileKey, status, videoId
            );
            return updated > 0;
        } catch (Exception e) {
            log.error("Failed to update video file key and status: {}", videoId, e);
            return false;
        }
    }

    private Date calcDateAfterMinutes(long minutes) {
        Date date = new Date();
        long expTimeMillis = date.getTime() + (1000 * 60 * minutes);
        date.setTime(expTimeMillis);
        return date;
    }

    @Override
    public boolean deleteVideo(UUID videoId) {
        try {
            // 1. DB에서 video_file_key 조회
            String fileKey = jdbcTemplate.queryForObject(
                    "SELECT video_file_key FROM video WHERE id = ?",
                    new Object[]{videoId}, String.class
            );

            // 2. S3에서 영상 파일 삭제 (파일이 없어도 S3 deleteObject는 예외 없이 성공 반환)
            if (fileKey != null) {
                try {
                    amazonS3Client.deleteObject(bucket, fileKey);
                    log.info("Deleted video file from S3: {}", fileKey);
                } catch (AmazonS3Exception e) {
                    log.warn("Failed to delete video file from S3 (continuing anyway): {}", fileKey, e);
                }
            }

            // 3. DB에서 status를 DELETED로 변경
            int updated = jdbcTemplate.update(
                    "UPDATE video SET status = ? WHERE id = ?",
                    VideoStatus.DELETED.name(), videoId
            );
            return updated > 0;
        } catch (EmptyResultDataAccessException e) {
            log.warn("Video not found for deletion: {}", videoId);
            return false;
        } catch (Exception e) {
            log.error("Failed to delete video: {}", videoId, e);
            return false;
        }
    }

    @Override
    public List<Video> findAllByMemberId(UUID memberId) {
        String sql = "SELECT id, storyboard_id, member_id, video_url, video_file_key, created_at, thumbnail_url, thumbnail_file_key, running_time, title, status FROM video WHERE member_id = ?";
        return jdbcTemplate.query(sql, new Object[]{memberId}, new BeanPropertyRowMapper<>(Video.class));
    }

    @Override
    public boolean updateRunningTime(UUID videoId, int runningTimeSeconds) {
        try {
            int updated = jdbcTemplate.update(
                    "UPDATE video SET running_time = ? WHERE id = ?",
                    runningTimeSeconds, videoId
            );
            return updated > 0;
        } catch (Exception e) {
            log.error("Failed to update running time: {}", videoId, e);
            return false;
        }
    }
}
