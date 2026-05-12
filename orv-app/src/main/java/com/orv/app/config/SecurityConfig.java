package com.orv.app.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Value("#{'${security.cors.allowed-origins}'.split(',')}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .logout(logout -> logout.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/public/**").permitAll()
                        .requestMatchers("/docs/**").permitAll()
                        .requestMatchers("/api/v0/auth/login/**").permitAll()
                        .requestMatchers("/api/v0/auth/callback/**").permitAll()
                        .requestMatchers("/api/v0/auth/nicknames").permitAll()
                        .requestMatchers("/api/v0/auth/join").permitAll()
                        .requestMatchers("/api/v0/auth/logout").permitAll()
                        .requestMatchers("/api/v0/archive/video/*").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/actuator/metrics/**").permitAll()
                        .requestMatchers("/actuator/prometheus/**").permitAll()
                        .requestMatchers("/api/v0/health").permitAll()
                        .requestMatchers("/api/v0/member/*/profile").permitAll()
                        .requestMatchers("/api/v0/reservation/interview/*").permitAll()
                        .requestMatchers("/api/v0/storyboard/*").permitAll()
                        .requestMatchers("/api/v0/storyboard/*/preview").permitAll()
                        .requestMatchers("/api/v0/storyboard/*/scene/all").permitAll()
                        .requestMatchers("/api/v0/storyboard/*/topic/list").permitAll()
                        .requestMatchers("/api/admin/**").hasAuthority("ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\": \"Unauthorized\"}");
                        })
                )
                .addFilterBefore(new OriginValidationFilter(normalizedAllowedOrigins()), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(normalizedAllowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private List<String> normalizedAllowedOrigins() {
        return allowedOrigins.stream()
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .collect(Collectors.toList());
    }
}
