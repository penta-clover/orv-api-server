package com.orv.api.domain.term;

import com.orv.api.domain.term.dto.TermAgreementForm;
import com.orv.api.global.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v0/term")
@Slf4j
public class TermController {
    private final TermRepository termRepository;

    @PostMapping("/agreement")
    public ApiResponse createAgreement(@RequestBody TermAgreementForm termAgreementForm, HttpServletRequest request) {
        try {
            String memberId = SecurityContextHolder.getContext().getAuthentication().getName();
            InetAddress ipAddress = InetAddress.getByName(getClientIp(request));
            Optional<String> agreementId = termRepository.saveAgreement(UUID.fromString(memberId), termAgreementForm.getTerm(), termAgreementForm.getValue(), ipAddress);

            if (agreementId.isEmpty()) {
                return ApiResponse.fail(null, 500);
            }

            return ApiResponse.success(agreementId.get(), 201);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.fail(null, 500);
        }
    }
    private String getClientIp(HttpServletRequest request) {
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
