package com.orv.api.domain.term;

import com.orv.api.domain.term.dto.TermAgreementForm;
import com.orv.api.global.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
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
public class TermController {
    private final TermRepository termRepository;

    @PostMapping("/agreement")
    public ApiResponse createAgreement(@RequestBody TermAgreementForm termAgreementForm, HttpServletRequest request) {
        try {
            String memberId = SecurityContextHolder.getContext().getAuthentication().getName();
            InetAddress ipAddress = InetAddress.getByName(request.getRemoteAddr());
            Optional<String> agreementId = termRepository.saveAgreement(UUID.fromString(memberId), termAgreementForm.getTerm(), termAgreementForm.getValue(), LocalDateTime.now(), ipAddress);

            if (agreementId.isEmpty()) {
                return ApiResponse.fail(null, 500);
            }

            return ApiResponse.success(agreementId.get(), 201);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.fail(null, 500);
        }
    }
}
