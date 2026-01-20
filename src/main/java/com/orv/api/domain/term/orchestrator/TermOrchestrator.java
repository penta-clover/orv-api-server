package com.orv.api.domain.term.orchestrator;

import com.orv.api.domain.term.controller.dto.TermAgreementRequest;
import com.orv.api.domain.term.service.TermService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.UnknownHostException;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TermOrchestrator {
    private final TermService termService;

    public Optional<String> createAgreement(UUID memberId, TermAgreementRequest request, String clientIp) throws UnknownHostException {
        return termService.createAgreement(memberId, request.getTerm(), request.getValue(), clientIp);
    }
}
