package com.orv.api.domain.archive;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.orv.api.domain.archive.dto.ImageMetadata;
import com.orv.api.domain.archive.dto.Video;
import com.orv.api.domain.archive.dto.VideoMetadata;
import com.orv.api.domain.storyboard.dto.Storyboard;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import java.io.InputStream;
import java.net.URI;
import java.util.*;

@Repository
public class S3VideoRepository implements VideoRepository {
    private final AmazonS3 amazonS3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.cloudfront.domain}")
    private String cloudfrontDomain;

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert simpleJdbcInsertVideo;

    public S3VideoRepository(AmazonS3 amazonS3Client, JdbcTemplate jdbcTemplate) {
        this.amazonS3Client = amazonS3Client;
        this.jdbcTemplate = jdbcTemplate;
        this.simpleJdbcInsertVideo = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("video")
                .usingColumns("id", "storyboard_id", "member_id", "video_url", "thumbnail_url", "running_time", "title");
    }

    public Optional<String> save(InputStream inputStream, VideoMetadata videoMetadata) {
        try {
            String fileId = UUID.randomUUID().toString();

            // AWS S3에 영상 업로드
            String fileUrl = "archive/videos/" + fileId;
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(videoMetadata.getContentType());
            metadata.setContentLength(videoMetadata.getContentLength());
            amazonS3Client.putObject(bucket, fileUrl, inputStream, metadata);
            URI downloadUri = URI.create(cloudfrontDomain + "/" + fileUrl);

            // DB에 영상 정보 저장
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("id", fileId);
            parameters.put("storyboard_id", videoMetadata.getStoryboardId().toString());
            parameters.put("member_id", videoMetadata.getOwnerId().toString());
            parameters.put("video_url", downloadUri.toString());
            parameters.put("thumbnail_url", "https://d3bdjeyz3ry3pi.cloudfront.net/static/images/default-archive-video-thumbnail.png");
            parameters.put("running_time", videoMetadata.getRunningTime());
            parameters.put("title", videoMetadata.getTitle());
            simpleJdbcInsertVideo.execute(new MapSqlParameterSource(parameters));

            return Optional.of(fileId.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @Override
    public Optional<Video> findById(UUID videoId) {
        String sql = "SELECT id, storyboard_id, member_id, video_url, created_at, thumbnail_url, running_time, title FROM video WHERE id = ?";

        try {
            Video video = jdbcTemplate.queryForObject(sql, new Object[]{videoId}, new BeanPropertyRowMapper<>(Video.class));
            return Optional.of(video);
        } catch (EmptyResultDataAccessException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @Override
    public List<Video> findByMemberId(UUID memberId, int offset, int limit) {
        String sql = "SELECT id, storyboard_id, member_id, video_url, created_at, thumbnail_url, running_time, title FROM video WHERE member_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?";

        try {
            List<Video> videos = jdbcTemplate.query(sql, new Object[]{memberId, limit, offset}, new BeanPropertyRowMapper<>(Video.class));
            return videos;
        } catch (EmptyResultDataAccessException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    @Override
    public boolean updateTitle(UUID videoId, String title) {
        try {
            jdbcTemplate.update("UPDATE video SET title = ? WHERE id = ?", title, videoId);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean updateThumbnail(UUID videoId, InputStream thumbnail, ImageMetadata imageMetadata) {
        try {
            String fileId = UUID.randomUUID().toString();

            // AWS S3에 영상 업로드
            String fileUrl = "archive/images/" + fileId;
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(imageMetadata.getContentType());
            metadata.setContentLength(imageMetadata.getContentLength());
            amazonS3Client.putObject(bucket, fileUrl, thumbnail, metadata);
            URI downloadUri = URI.create(cloudfrontDomain + "/" + fileUrl);

            // DB에 썸네일 url 업데이트
            jdbcTemplate.update("UPDATE video SET thumbnail_url = ? WHERE id = ?", downloadUri.toString(), videoId);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
