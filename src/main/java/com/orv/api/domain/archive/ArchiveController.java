package com.orv.api.domain.archive;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.orv.api.global.dto.ApiResponse;
import com.orv.api.global.dto.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v0/archive")
@RequiredArgsConstructor
public class ArchiveController {
    private final AmazonS3Client amazonS3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @PostMapping("/recorded-video")
    public ApiResponse uploadRecordedVideo(@RequestParam("video") MultipartFile video) {
        try {
            String fileName = UUID.randomUUID().toString();
            String fileUrl = "https://" + bucket + "/archive/videos/" + fileName;
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentType(video.getContentType());
            objectMetadata.setContentLength(video.getSize());

            amazonS3Client.putObject(bucket, fileUrl, video.getInputStream(), objectMetadata);
            return ApiResponse.success(fileName, 201);
        } catch (IOException e) {
            e.printStackTrace();
            return ApiResponse.fail(ErrorCode.UNKNOWN, 500);
        }

    }


}
