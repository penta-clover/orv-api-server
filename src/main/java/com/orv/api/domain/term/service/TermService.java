package com.orv.api.domain.term.service;

import com.orv.api.domain.term.repository.TermRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TermService {
    private final TermRepository termRepository;

    public Optional<String> createAgreement(UUID memberId, String term, String value, String clientIp) throws UnknownHostException {
        InetAddress ipAddress = InetAddress.getByName(clientIp);
        return termRepository.saveAgreement(memberId, term, value, ipAddress);
    }
}
