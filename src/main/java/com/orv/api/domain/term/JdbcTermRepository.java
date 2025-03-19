package com.orv.api.domain.term;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcTermRepository implements TermRepository {
    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert simpleJdbcInsert;

    public JdbcTermRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("term_agreement")
                .usingColumns("member_id", "term", "value", "agreed_at", "ip_address")
                .usingGeneratedKeyColumns("id");
    }

    @Override
    public Optional<String> saveAgreement(UUID memberId, String term, String value, LocalDateTime agreed_at, InetAddress ip) {
        Map<String, Object> params = new HashMap<>();

        params.put("member_id", memberId);
        params.put("term", term);
        params.put("value", value);
        params.put("agreed_at", agreed_at);
        params.put("ip_address", ip);

        KeyHolder keyHolder = simpleJdbcInsert.executeAndReturnKeyHolder(params);
        String agreementId = keyHolder.getKey().toString();
        return Optional.of(agreementId);
    }
}
