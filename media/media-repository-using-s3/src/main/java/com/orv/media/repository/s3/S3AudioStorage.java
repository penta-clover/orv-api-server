package com.orv.media.repository.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.InputStream;
import java.util.UUID;
import com.orv.media.repository.AudioStorage;

@Repository
@Slf4j
public class S3AudioStorage implements AudioStorage {
    private final AmazonS3 amazonS3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public S3AudioStorage(AmazonS3 amazonS3Client) {
        this.amazonS3Client = amazonS3Client;
    }

    @Override
    public String save(InputStream audioStream, String contentType, long contentLength) {
        UUID fileId = UUID.randomUUID();
        String fileKey = "archive/audios/" + fileId;

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);
        metadata.setContentLength(contentLength);

        try {
            amazonS3Client.putObject(bucket, fileKey, audioStream, metadata);
            log.info("Uploaded audio file to S3: {}", fileKey);
            return fileKey;
        } catch (Exception e) {
            log.error("Failed to upload audio file to S3: {}", fileKey, e);
            throw new RuntimeException("Failed to upload audio to S3", e);
        }
    }

    @Override
    public void delete(String fileKey) {
        try {
            amazonS3Client.deleteObject(bucket, fileKey);
            log.info("Deleted audio file from S3: {}", fileKey);
        } catch (Exception e) {
            log.error("Failed to delete audio file from S3: {}. Manual cleanup may be required.", fileKey, e);
            throw new RuntimeException("Failed to delete audio from S3", e);
        }
    }
}
