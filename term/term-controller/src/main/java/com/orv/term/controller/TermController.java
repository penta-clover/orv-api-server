package com.orv.term.controller;

import com.orv.term.controller.dto.TermAgreementRequest;
import com.orv.term.orchestrator.TermOrchestrator;
import com.orv.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v0/term")
@Slf4j
public class TermController {
    private final TermOrchestrator termOrchestrator;

    @PostMapping("/agreement")
    public ApiResponse createAgreement(@RequestBody TermAgreementRequest termAgreementRequest, HttpServletRequest request) {
        String memberId = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            String clientIp = extractClientIp(request);

            Optional<String> agreementId = termOrchestrator.createAgreement(
                    UUID.fromString(memberId),
                    termAgreementRequest.getTerm(),
                    termAgreementRequest.getValue(),
                    clientIp
            );

            if (agreementId.isEmpty()) {
                return ApiResponse.fail(null, 500);
            }

            return ApiResponse.success(agreementId.get(), 201);
        } catch (Exception e) {
            log.error("createAgreement failed memberId={}", memberId, e);
            return ApiResponse.fail(null, 500);
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress != null && !ipAddress.isEmpty() && !"unknown".equalsIgnoreCase(ipAddress)) {
            // 여러 IP가 있을 경우 첫 번째 IP가 클라이언트 IP입니다.
            ipAddress = ipAddress.split(",")[0].trim();
        } else {
            ipAddress = request.getHeader("X-Real-IP");
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getRemoteAddr();
            }
        }
        return ipAddress;
    }
}
