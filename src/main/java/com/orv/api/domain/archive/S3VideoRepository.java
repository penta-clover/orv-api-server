package com.orv.api.domain.archive;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class S3VideoRepository implements VideoRepository {
    private final AmazonS3Client amazonS3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.cloudfront.domain}")
    private String cloudfrontDomain;

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert simpleJdbcInsertVideo;

    public S3VideoRepository(AmazonS3Client amazonS3Client, JdbcTemplate jdbcTemplate) {
        this.amazonS3Client = amazonS3Client;
        this.jdbcTemplate = jdbcTemplate;
        this.simpleJdbcInsertVideo = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("video")
                .usingColumns("id", "storyboard_id", "member_id", "video_url", "thumbnail_url", "title");
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
            parameters.put("thumbnail_url", cloudfrontDomain + "/static/images/default-archive-video-thumbnail.png");
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
        String sql = "SELECT id, storyboard_id, member_id, video_url, created_at, thumbnail_url, title FROM storyboard WHERE id = ?";

        try {
            Video video = jdbcTemplate.queryForObject(sql, new Object[]{videoId}, new BeanPropertyRowMapper<>(Video.class));
            return Optional.of(video);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean updateTitle(String videoId, String title) {
        try {
            jdbcTemplate.update("UPDATE video SET title = ? WHERE id = ?", title, UUID.fromString(videoId));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean updateThumbnail(InputStream inputStream, String videoId) {
        return false;
    }
}
