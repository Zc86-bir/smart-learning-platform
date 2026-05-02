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

        // Try to get user info from JWT filter (set as request attributes)
        Object userIdAttr = request.getAttribute("userId");
        Object userRoleAttr = request.getAttribute("userRole");

        // Fallback to X-User-Id and X-User-Role headers for backward compatibility
        if (userIdAttr == null) {
            var userIdHeader = request.getHeader("X-User-Id");
            if (userIdHeader != null && !userIdHeader.isBlank()) {
                userIdAttr = Long.parseLong(userIdHeader);
            }
        }
        if (userRoleAttr == null) {
            var userRoleHeader = request.getHeader("X-User-Role");
            if (userRoleHeader != null && !userRoleHeader.isBlank()) {
                userRoleAttr = userRoleHeader.trim();
            }
        }

        if (userIdAttr == null) {
            throw new BizException(401, "未登录或登录已过期");
        }
        if (userRoleAttr == null) {
            throw new BizException(401, "Missing user role");
        }

        long uid = ((Number) userIdAttr).longValue();
        var trimmedRole = ((String) userRoleAttr).trim();

        if (!VALID_ROLES.contains(trimmedRole)) {
            throw new BizException(401, "Invalid role: " + userRoleAttr);
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
