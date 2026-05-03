package com.smartlearn.platform.annotation;

import java.lang.annotation.*;

/**
 * Marks a method for automatic operation audit logging.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogOperation {
    String module() default "";
    String operation() default "";
}
