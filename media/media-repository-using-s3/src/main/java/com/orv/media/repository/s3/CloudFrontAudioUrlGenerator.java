package com.orv.media.repository.s3;

import com.orv.media.repository.PublicAudioUrlGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CloudFrontAudioUrlGenerator implements PublicAudioUrlGenerator {

    @Value("${cloud.aws.cloudfront.domain}")
    private String cloudfrontDomain;

    @Override
    public String generateUrl(String fileKey) {
        return cloudfrontDomain + "/" + fileKey;
    }
}
