package com.orv.api.domain.media.repository;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.InputStream;
import java.util.UUID;

@Repository
@Slf4j
public class S3AudioStorage implements AudioStorage {
    private final AmazonS3 amazonS3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.cloudfront.domain}")
    private String cloudfrontDomain;

    public S3AudioStorage(AmazonS3 amazonS3Client) {
        this.amazonS3Client = amazonS3Client;
    }

    @Override
    public UUID save(InputStream audioStream, String contentType, long contentLength) {
        UUID fileId = UUID.randomUUID();
        String s3Path = "archive/audios/" + fileId;

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);
        metadata.setContentLength(contentLength);

        try {
            amazonS3Client.putObject(bucket, s3Path, audioStream, metadata);
            log.info("Uploaded audio file to S3: {}", s3Path);
            return fileId;
        } catch (Exception e) {
            log.error("Failed to upload audio file to S3: {}", s3Path, e);
            throw new RuntimeException("Failed to upload audio to S3", e);
        }
    }

    @Override
    public void delete(UUID fileId) {
        String s3Path = "archive/audios/" + fileId;

        try {
            amazonS3Client.deleteObject(bucket, s3Path);
            log.info("Deleted audio file from S3: {}", s3Path);
        } catch (Exception e) {
            log.error("Failed to delete audio file from S3: {}. Manual cleanup may be required.", s3Path, e);
            throw new RuntimeException("Failed to delete audio from S3", e);
        }
    }

    @Override
    public String getUrl(UUID fileId) {
        return cloudfrontDomain + "/archive/audios/" + fileId;
    }
}
