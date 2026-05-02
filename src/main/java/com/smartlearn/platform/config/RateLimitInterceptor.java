package com.smartlearn.platform.config;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Rate limiter interceptor — blocks IPs making excessive requests.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final int MAX_REQUESTS_PER_MINUTE = 120;
    private static final String RATE_LIMIT_KEY = "rate:limit:";

    private final StringRedisTemplate redisTemplate;

    public RateLimitInterceptor(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Skip rate limiting for health checks and static assets
        var path = request.getRequestURI();
        if (path.startsWith("/actuator") || path.startsWith("/swagger")) {
            return true;
        }

        var ip = getClientIp(request);
        var key = RATE_LIMIT_KEY + ip;
        var count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, 60, java.util.concurrent.TimeUnit.SECONDS);
        }

        if (count != null && count > MAX_REQUESTS_PER_MINUTE) {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            try {
                response.getWriter().write("{\"code\":429,\"message\":\"请求过于频繁，请稍后重试\"}");
            } catch (java.io.IOException ignored) {}
            return false;
        }

        // Store IP for downstream use
        request.setAttribute("clientIp", ip);
        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        var forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        var realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }
        return request.getRemoteAddr();
    }
}
