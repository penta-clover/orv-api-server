package com.orv.api.domain.health.controller;

import com.orv.api.domain.health.controller.dto.HealthStatusResponse;
import com.orv.api.domain.health.orchestrator.HealthOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v0/health")
@RequiredArgsConstructor
public class HealthController {
    private final HealthOrchestrator healthOrchestrator;

    @GetMapping
    public HealthStatusResponse getHealth() {
        return healthOrchestrator.getHealthStatus();
    }
}
