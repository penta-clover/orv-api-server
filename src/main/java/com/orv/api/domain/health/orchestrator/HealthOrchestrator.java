package com.orv.api.domain.health.orchestrator;

import com.orv.api.domain.health.controller.dto.HealthStatusResponse;
import org.springframework.stereotype.Component;

@Component
public class HealthOrchestrator {

    public HealthStatusResponse getHealthStatus() {
        return new HealthStatusResponse("this server is healthy");
    }
}
