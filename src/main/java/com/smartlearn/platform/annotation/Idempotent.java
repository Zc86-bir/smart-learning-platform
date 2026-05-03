package com.smartlearn.platform.annotation;

import java.lang.annotation.*;

/**
 * Marks an API endpoint as idempotent.
 * Uses a client-provided idempotency key (header: X-Idempotency-Key)
 * to detect and reject duplicate requests within a TTL window.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {
    /** Redis key prefix for idempotency tracking */
    String prefix() default "idempotent:";
    /** TTL in seconds for the idempotency key */
    long ttlSeconds() default 300;
    /** Custom message when a duplicate request is detected */
    String message() default "请勿重复提交";
}
