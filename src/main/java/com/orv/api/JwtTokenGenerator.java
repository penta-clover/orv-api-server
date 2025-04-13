package com.orv.api;

import com.orv.api.domain.auth.JwtTokenProvider;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

@Component
@Slf4j
public class JwtTokenGenerator {
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

//    // For generating JWT tokens for testing purposes
//    @PostConstruct
//    public void afterInit() {
//        log.info(jwtTokenProvider.createToken(
//                "7872de70-9bed-45a1-80f3-ea9666ba87f3",
//                Map.of(
//                        "provider", "local",
//                        "socialId", "1234567890",
//                        "roles", Collections.emptyList()
//                ),
//                300000000000L
//        ));
//    }
}
