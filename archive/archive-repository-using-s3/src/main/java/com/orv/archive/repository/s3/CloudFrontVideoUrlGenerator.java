package com.orv.archive.repository.s3;

import com.orv.archive.service.PublicVideoUrlGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CloudFrontVideoUrlGenerator implements PublicVideoUrlGenerator {

    private final String cloudfrontDomain;

    public CloudFrontVideoUrlGenerator(@Value("${cloud.aws.cloudfront.domain}") String cloudfrontDomain) {
        this.cloudfrontDomain = cloudfrontDomain;
    }

    @Override
    public String generateUrl(String fileKey) {
        return cloudfrontDomain + "/" + fileKey;
    }
}
