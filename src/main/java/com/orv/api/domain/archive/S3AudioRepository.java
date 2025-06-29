package com.orv.api.domain.archive;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.orv.api.domain.archive.dto.AudioMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.InputStream;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;

@Repository
public class S3AudioRepository implements AudioRepository {
    private final AmazonS3 amazonS3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.cloudfront.domain}")
    private String cloudfrontDomain;

    public S3AudioRepository(AmazonS3 amazonS3Client) {
        this.amazonS3Client = amazonS3Client;
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

            return Optional.of(downloadUri.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }
}