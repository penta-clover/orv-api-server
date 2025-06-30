package com.orv.api.domain.archive;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.orv.api.domain.archive.dto.AudioMetadata;
import org.springframework.beans.factory.annotation.Value;
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
public class S3AudioRepository implements AudioRepository {
    private final AmazonS3 amazonS3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.cloudfront.domain}")
    private String cloudfrontDomain;

    private final SimpleJdbcInsert simpleJdbcInsertAudio;

    public S3AudioRepository(AmazonS3 amazonS3Client, JdbcTemplate jdbcTemplate) {
        this.amazonS3Client = amazonS3Client;
        this.simpleJdbcInsertAudio = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("audio")
                .usingColumns("id", "storyboard_id", "member_id", "audio_url", "running_time");
    }

    @Override
    public Optional<String> save(InputStream inputStream, AudioMetadata audioMetadata) {
        try {
            String fileId = UUID.randomUUID().toString();
            String fileUrl = "archive/audios/" + fileId;

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(audioMetadata.getContentType());
            metadata.setContentLength(audioMetadata.getContentLength());
            amazonS3Client.putObject(bucket, fileUrl, inputStream, metadata);
            URI downloadUri = URI.create(cloudfrontDomain + "/" + fileUrl);

            // DB에 오디오 정보 저장
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("id", fileId);
            parameters.put("storyboard_id", audioMetadata.getStoryboardId().toString());
            parameters.put("member_id", audioMetadata.getOwnerId().toString());
            parameters.put("audio_url", downloadUri.toString());
            parameters.put("running_time", audioMetadata.getRunningTime());
            simpleJdbcInsertAudio.execute(new MapSqlParameterSource(parameters));

            return Optional.of(downloadUri.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }
}