package com.smartlearn.platform.interceptor;

import com.smartlearn.platform.entity.User;
import com.smartlearn.platform.exception.BizException;
import com.smartlearn.platform.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.Set;

@Component
public class SecurityInterceptor implements HandlerInterceptor {

    private static final Set<String> VALID_ROLES = Set.of("STUDENT", "ADMIN");

    private final UserMapper userMapper;

    public SecurityInterceptor(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Skip auth for @PermitAll endpoints
        if (handler instanceof HandlerMethod hm) {
            var methodAnnotation = hm.getMethodAnnotation(PermitAll.class);
            var classAnnotation = hm.getBeanType().getAnnotation(PermitAll.class);
            if (methodAnnotation != null || classAnnotation != null) {
                return true;
            }
        }

        var userId = request.getHeader("X-User-Id");
        var userRole = request.getHeader("X-User-Role");

        if (userId == null || userId.isBlank()) {
            throw new BizException(401, "Missing X-User-Id header");
        }
        if (userRole == null || userRole.isBlank()) {
            throw new BizException(401, "Missing X-User-Role header");
        }

        // Parse userId eagerly to catch invalid values early
        long uid;
        try {
            uid = Long.parseLong(userId);
        } catch (NumberFormatException e) {
            throw new BizException(401, "Invalid X-User-Id: must be a number");
        }

        var trimmedRole = userRole.trim();
        if (!VALID_ROLES.contains(trimmedRole)) {
            throw new BizException(401, "Invalid role: " + userRole);
        }

        // Validate user exists and role matches in database
        User user = userMapper.selectById(uid);
        if (user == null) {
            throw new BizException(401, "用户不存在");
        }
        if (!user.getRole().equals(trimmedRole)) {
            throw new BizException(403, "角色不匹配：数据库记录为 " + user.getRole());
        }

        request.setAttribute("userId", uid);
        request.setAttribute("userRole", trimmedRole);

        // Role-based endpoint check via @RequireRole
        if (handler instanceof HandlerMethod hm) {
            var methodAnnotation = hm.getMethodAnnotation(RequireRole.class);
            var classAnnotation = hm.getBeanType().getAnnotation(RequireRole.class);

            var allowed = methodAnnotation != null ? methodAnnotation : classAnnotation;
            if (allowed != null) {
                var roles = Arrays.asList(allowed.value());
                if (!roles.contains(trimmedRole)) {
                    throw new BizException(403, "Access denied: requires " + roles + ", got " + trimmedRole);
                }
            }
        }

        return true;
    }
}
