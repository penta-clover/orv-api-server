package com.orv.app.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class SessionCookieConfigTest {
    @Test
    void cookieSerializer_setsPersistentSecureHttpOnlySameSiteCookie() {
        SessionCookieConfig config = new SessionCookieConfig();
        ReflectionTestUtils.setField(config, "cookieName", "ORVSESSION");
        ReflectionTestUtils.setField(config, "secureCookie", true);
        ReflectionTestUtils.setField(config, "sameSite", "Lax");
        ReflectionTestUtils.setField(config, "cookieMaxAge", Duration.ofDays(14));

        CookieSerializer serializer = config.cookieSerializer();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        serializer.writeCookieValue(new CookieSerializer.CookieValue(request, response, "session-id"));

        assertThat(response.getHeader("Set-Cookie"))
                .contains("ORVSESSION=")
                .contains("Max-Age=1209600")
                .contains("Secure")
                .contains("HttpOnly")
                .contains("SameSite=Lax");
    }
}
