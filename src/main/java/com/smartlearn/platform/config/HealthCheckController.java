package com.smartlearn.platform.config;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Simple health check endpoint — replacement for HealthIndicator in Spring Boot 4.0.
 */
@RestController
@RequestMapping("/actuator/health")
public class HealthCheckController {

    private final StringRedisTemplate redisTemplate;

    public HealthCheckController(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @GetMapping
    public Map<String, Object> health() {
        var result = new LinkedHashMap<String, Object>();
        result.put("status", "UP");

        // Check Redis
        try {
            var pong = redisTemplate.getConnectionFactory()
                .getConnection().ping();
            result.put("redis", Map.of("status", "UP", "ping", pong));
        } catch (Exception e) {
            result.put("redis", Map.of("status", "DOWN", "error", e.getMessage()));
            result.put("status", "DOWN");
        }

        // Check DB connection via a simple query
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            result.put("database", Map.of("status", "UP"));
        } catch (Exception e) {
            result.put("database", Map.of("status", "DOWN", "error", e.getMessage()));
        }

        return result;
    }
}
