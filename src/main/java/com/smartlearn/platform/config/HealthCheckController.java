package com.smartlearn.platform.config;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * System health check: verifies Redis and Database connectivity.
 */
@RestController
@RequestMapping("/actuator/health")
public class HealthCheckController {

    private final StringRedisTemplate redisTemplate;
    private final JdbcTemplate jdbcTemplate;

    public HealthCheckController(StringRedisTemplate redisTemplate, JdbcTemplate jdbcTemplate) {
        this.redisTemplate = redisTemplate;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public Map<String, Object> health() {
        var result = new LinkedHashMap<String, Object>();
        boolean allUp = true;

        // Check Redis
        try {
            var pong = redisTemplate.getConnectionFactory().getConnection().ping();
            result.put("redis", Map.of("status", "UP", "ping", pong));
        } catch (Exception e) {
            result.put("redis", Map.of("status", "DOWN", "error", e.getMessage()));
            allUp = false;
        }

        // Check Database
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            result.put("database", Map.of("status", "UP"));
        } catch (Exception e) {
            result.put("database", Map.of("status", "DOWN", "error", e.getMessage()));
            allUp = false;
        }

        result.put("status", allUp ? "UP" : "DOWN");
        return result;
    }
}
