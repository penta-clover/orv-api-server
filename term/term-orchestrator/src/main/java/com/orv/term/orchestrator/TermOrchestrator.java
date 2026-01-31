package com.orv.term.orchestrator;

import com.orv.term.service.TermService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.UnknownHostException;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TermOrchestrator {
    private final TermService termService;

    public Optional<String> createAgreement(UUID memberId, String term, String value, String clientIp) throws UnknownHostException {
        return termService.createAgreement(memberId, term, value, clientIp);
    }
}
