package com.smartlearn.platform.validator;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validates password strength: min 8 chars, at least one uppercase,
 * one lowercase, one digit, one special character.
 */
@Documented
@Constraint(validatedBy = PasswordStrength.PasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordStrength {
    String message() default "密码强度不足: 至少8位,包含大小写字母、数字和特殊字符";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class PasswordValidator implements ConstraintValidator<PasswordStrength, String> {
        @Override
        public boolean isValid(String password, ConstraintValidatorContext ctx) {
            if (password == null) return false;
            if (password.length() < 8) return false;
            if (!password.matches(".*[A-Z].*")) return false;
            if (!password.matches(".*[a-z].*")) return false;
            if (!password.matches(".*\\d.*")) return false;
            if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) return false;
            return true;
        }
    }
}
