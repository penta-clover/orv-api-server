package com.orv.archive.repository.s3.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class S3Config {
    @Value("${cloud.aws.credentials.accessKey}")
    private String accessKey;
    @Value("${cloud.aws.credentials.secretKey}")
    private String secretKey;
    @Value("${cloud.aws.region.static}")
    private String region;

    // 비어있으면(기본값) 실제 AWS S3로 접속한다 — 기존 동작과 완전히 동일.
    // 값이 있으면(MinIO 등 S3 호환 스토리지) 그 엔드포인트로 접속하고 path-style을 켠다.
    // LocalStackS3Config(@Profile("staging"))가 하던 일을 프로파일 분기 없이
    // 환경변수 하나로 통합한 것 — 2026-08-27 경로 A 채택.
    @Value("${cloud.aws.s3.endpoint:}")
    private String endpoint;

    @Bean
    @ConditionalOnMissingBean(AmazonS3.class)
    public AmazonS3 amazonS3Client() {
        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials));

        if (StringUtils.hasText(endpoint)) {
            builder.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, region))
                    .withPathStyleAccessEnabled(true);
        } else {
            builder.withRegion(region).enableDualstack();
        }

        return builder.build();
    }
}
