package com.orv.api;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.PostMapping;

import com.orv.api.domain.auth.service.JwtTokenService;

@TestConfiguration
public class JwtTokenServiceTestConfig {
    @Bean
    public JwtTokenService jwtTokenService() {
        return new JwtTokenService();
    }
}
