package com.orv.api.domain.health.controller;

import com.orv.api.domain.health.service.dto.HealthStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v0/health")
public class HealthController {
    @GetMapping
    public HealthStatus getHealth() {
        HealthStatus status = new HealthStatus();
        status.setMsg("this server is healthy");
        return status;
    }
}
