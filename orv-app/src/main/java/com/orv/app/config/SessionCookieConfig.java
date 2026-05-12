package com.orv.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

import java.time.Duration;

@Configuration
public class SessionCookieConfig {
    @Value("${server.servlet.session.cookie.name:ORVSESSION}")
    private String cookieName;

    @Value("${server.servlet.session.cookie.secure:true}")
    private boolean secureCookie;

    @Value("${server.servlet.session.cookie.same-site:Lax}")
    private String sameSite;

    @Value("${server.servlet.session.cookie.max-age:14d}")
    private Duration cookieMaxAge;

    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName(cookieName);
        serializer.setCookiePath("/");
        serializer.setUseHttpOnlyCookie(true);
        serializer.setUseSecureCookie(secureCookie);
        serializer.setSameSite(sameSite);
        serializer.setCookieMaxAge(Math.toIntExact(cookieMaxAge.getSeconds()));
        return serializer;
    }
}
