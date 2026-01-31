package com.orv.health.orchestrator;

import com.orv.health.orchestrator.dto.HealthStatusResponse;
import org.springframework.stereotype.Component;

@Component
public class HealthOrchestrator {

    public HealthStatusResponse getHealthStatus() {
        return new HealthStatusResponse("this server is healthy");
    }
}
