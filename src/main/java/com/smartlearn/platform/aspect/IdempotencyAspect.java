package com.smartlearn.platform.aspect;

import com.smartlearn.platform.annotation.Idempotent;
import com.smartlearn.platform.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

/**
 * AOP aspect that enforces idempotency on endpoints annotated with @Idempotent.
 * Clients must send an X-Idempotency-Key header. Duplicate keys within the TTL
 * window will be rejected with a BizException.
 */
@Aspect
@Component
@Slf4j
public class IdempotencyAspect {

    private final StringRedisTemplate redisTemplate;

    public IdempotencyAspect(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint point, Idempotent idempotent) throws Throwable {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return point.proceed();
        }

        HttpServletRequest request = attrs.getRequest();
        String idempotencyKey = request.getHeader("X-Idempotency-Key");
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BizException("请提供幂等键: X-Idempotency-Key");
        }

        String redisKey = idempotent.prefix() + idempotencyKey.trim();
        Boolean exists = redisTemplate.hasKey(redisKey);
        if (Boolean.TRUE.equals(exists)) {
            log.warn("[Idempotency] Duplicate request rejected: key={}", idempotencyKey);
            throw new BizException(idempotent.message());
        }

        // Mark the key as processed before executing
        redisTemplate.opsForValue().set(redisKey, "1", idempotent.ttlSeconds(), TimeUnit.SECONDS);

        try {
            return point.proceed();
        } catch (Exception e) {
            // Rollback: remove the key so client can retry
            redisTemplate.delete(redisKey);
            throw e;
        }
    }
}
