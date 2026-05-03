package com.smartlearn.platform.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final OAuth2LoginSuccessHandler oauth2LoginSuccessHandler;

    @Value("${spring.security.oauth2.client.registration.github.client-id:}")
    private String githubClientId;

    public SecurityConfig(OAuth2LoginSuccessHandler oauth2LoginSuccessHandler) {
        this.oauth2LoginSuccessHandler = oauth2LoginSuccessHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/oauth2/**", "/login/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .anyRequest().permitAll() // Let SecurityInterceptor handle auth
            );

        // Only enable OAuth2 login when github client-id is configured
        if (githubClientId != null && !githubClientId.isBlank()) {
            http.oauth2Login(oauth2 -> oauth2
                .successHandler(oauth2LoginSuccessHandler)
            );
        }

        http.logout(logout -> logout.disable());

        return http.build();
    }
}
