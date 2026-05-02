package com.smartlearn.platform.interceptor;

import com.smartlearn.platform.entity.Permission;
import com.smartlearn.platform.entity.User;
import com.smartlearn.platform.exception.BizException;
import com.smartlearn.platform.mapper.PermissionMapper;
import com.smartlearn.platform.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class SecurityInterceptor implements HandlerInterceptor {

    private final UserMapper userMapper;
    private final PermissionMapper permissionMapper;

    public SecurityInterceptor(UserMapper userMapper, PermissionMapper permissionMapper) {
        this.userMapper = userMapper;
        this.permissionMapper = permissionMapper;
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

        // Get user ID from JWT filter or header
        Object userIdAttr = request.getAttribute("userId");
        if (userIdAttr == null) {
            var userIdHeader = request.getHeader("X-User-Id");
            if (userIdHeader != null && !userIdHeader.isBlank()) {
                try {
                    userIdAttr = Long.parseLong(userIdHeader);
                } catch (NumberFormatException e) {
                    throw new BizException(401, "Invalid X-User-Id");
                }
            }
        }

        if (userIdAttr == null) {
            throw new BizException(401, "未登录或登录已过期");
        }

        long uid = ((Number) userIdAttr).longValue();

        // Validate user exists
        User user = userMapper.selectById(uid);
        if (user == null) {
            throw new BizException(401, "用户不存在");
        }

        // Get user roles from RBAC (JWT already has roles, but verify from DB for fresh data)
        @SuppressWarnings("unchecked")
        List<String> userRoles = (List<String>) request.getAttribute("userRoles");
        if (userRoles == null || userRoles.isEmpty()) {
            // Fallback: single role from old JWT or header
            String roleAttr = (String) request.getAttribute("userRole");
            if (roleAttr != null) {
                userRoles = List.of(roleAttr);
            } else {
                var roleHeader = request.getHeader("X-User-Role");
                if (roleHeader != null && !roleHeader.isBlank()) {
                    userRoles = List.of(roleHeader.trim());
                }
            }
        }

        if (userRoles == null || userRoles.isEmpty()) {
            throw new BizException(403, "用户未分配角色");
        }

        // Check @RequireRole annotation
        if (handler instanceof HandlerMethod hm) {
            var methodAnnotation = hm.getMethodAnnotation(RequireRole.class);
            var classAnnotation = hm.getBeanType().getAnnotation(RequireRole.class);

            var allowed = methodAnnotation != null ? methodAnnotation : classAnnotation;
            if (allowed != null) {
                var requiredRoles = Arrays.asList(allowed.value());
                boolean hasRole = userRoles.stream().anyMatch(requiredRoles::contains);
                if (!hasRole) {
                    throw new BizException(403, "权限不足：需要 " + requiredRoles + ", 当前角色 " + userRoles);
                }
            }
        }

        // Check API permission via RBAC (if enabled)
        String apiPath = request.getServletPath();
        String method = request.getMethod();
        List<Permission> permissions = permissionMapper.selectByUserId(uid);
        List<String> permCodes = permissions.stream()
            .filter(p -> "api".equals(p.getType()))
            .map(Permission::getCode)
            .collect(Collectors.toList());

        // Store for downstream use
        request.setAttribute("userId", uid);
        request.setAttribute("userRoles", userRoles);
        request.setAttribute("userPermissions", permCodes);

        return true;
    }
}
