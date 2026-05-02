package com.smartlearn.platform.config;

import com.smartlearn.platform.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    @Bean
    public JwtUtil jwtUtil() {
        return new JwtUtil(secret, expirationMs);
    }
}
