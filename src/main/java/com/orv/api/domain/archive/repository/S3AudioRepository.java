package com.orv.api.domain.archive.repository;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.orv.api.domain.archive.service.dto.AudioMetadata;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.UUID;

@Repository
@Slf4j
public class S3AudioRepository implements AudioRepository {
    private final AmazonS3 amazonS3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public S3AudioRepository(AmazonS3 amazonS3Client) {
        this.amazonS3Client = amazonS3Client;
    }

    @Override
    public Optional<URI> save(InputStream inputStream, AudioMetadata audioMetadata) {
        try {
            String fileId = UUID.randomUUID().toString();
            String filePath = "archive/audios/" + fileId;

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(audioMetadata.getContentType());
            metadata.setContentLength(audioMetadata.getContentLength());
            amazonS3Client.putObject(bucket, filePath, inputStream, metadata);

            URI s3Uri = new URI("s3", bucket, "/" + filePath, null);
            return Optional.of(s3Uri);
        } catch (URISyntaxException e) {
            log.error("S3 URI 생성 실패", e);
            return Optional.empty();
        } catch (Exception e) {
            log.error("S3에 오디오 파일 업로드 중 오류 발생", e);
            return Optional.empty();
        }
    }
}
