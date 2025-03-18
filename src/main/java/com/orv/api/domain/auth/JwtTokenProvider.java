package com.orv.api.domain.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Slf4j
@Service
public class JwtTokenProvider {
    @Value("${security.jwt.secret}")
    private String secretKey;

    private long validityInMilliseconds = 604800000L; // 1 week

    @PostConstruct
    protected void init() {
        log.info("jwt token only for test: {}", createToken("054c3e8a-3387-4eb3-ac8a-31a48221f192", Map.of("provider", "google", "socialId", "111622496717434972547"), 604800000L * 52 * 10));
    }

    public String createToken(String subject, Map<String, ?> claims) {
        return createToken(subject, claims, this.validityInMilliseconds);
    }

    public String createToken(String subject, Map<String, ?> claims, long validityInMilliseconds) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(parseJwtSecretKey(secretKey), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(parseJwtSecretKey(secretKey))
                    .build()
                    .parseClaimsJws(token);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Map<String, Object> getPayload(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(parseJwtSecretKey(secretKey))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private SecretKey parseJwtSecretKey(String keyString) {
        byte[] keyBytes = keyString.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
