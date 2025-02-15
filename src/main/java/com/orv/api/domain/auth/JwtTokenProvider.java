package com.orv.api.domain.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

@Slf4j
@Service
public class JwtTokenProvider {
    @Value("${security.jwt.secret}")
    private String secretKey;

    private long validityInMilliseconds = 604800000; // 1 week

    public String createToken(String subject, Map<String, ?> claims) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
                .setSubject(subject)
                .setClaims(claims)
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

    public Map<String, ?> getPayload(String token) {
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
