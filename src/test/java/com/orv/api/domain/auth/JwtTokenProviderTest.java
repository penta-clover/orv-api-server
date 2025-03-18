package com.orv.api.domain.auth;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        // HS256 알고리즘 사용 시 최소 32바이트 이상의 secret key 필요
        "security.jwt.secret=my-very-secret-key-that-is-at-least-32-bytes-long!"
})
class JwtTokenProviderTest {
        @Autowired
        private JwtTokenProvider jwtTokenProvider;

        @Test
        void testCreateAndValidateToken() {
            // given
            String subject = "testUser";
            Map<String, Object> claims = Map.of("role", "USER");

            // when
            String token = jwtTokenProvider.createToken(subject, claims);

            // then: 토큰 생성 확인
            assertNotNull(token, "생성된 토큰은 null이 아니어야 합니다.");
            // then: 생성된 토큰이 유효해야 합니다.
            assertTrue(jwtTokenProvider.validateToken(token), "토큰이 유효해야 합니다.");

            // then: 페이로드에서 subject 및 클레임 값 추출
            Map<String, ?> payload = jwtTokenProvider.getPayload(token);
            // JWT의 subject는 "sub"라는 이름으로 저장됩니다.
            assertEquals(subject, payload.get("sub"), "토큰의 subject가 일치해야 합니다.");
            assertEquals("USER", payload.get("role"), "토큰에 저장된 role 클레임이 일치해야 합니다.");
    }
}
