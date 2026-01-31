package com.orv.term.repository;

import org.springframework.stereotype.Repository;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TermRepository {
    Optional<String> saveAgreement(UUID memberId, String term, String value, InetAddress ip);
}
