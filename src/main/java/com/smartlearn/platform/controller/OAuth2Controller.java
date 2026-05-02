package com.smartlearn.platform.controller;

import com.smartlearn.platform.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/oauth2")
@Tag(name = "OAuth2登录", description = "第三方单点登录")
public class OAuth2Controller {

    @Value("${server.servlet.context-path:/}")
    private String contextPath;

    @GetMapping("/providers")
    @Operation(summary = "获取支持的第三方登录")
    public ApiResponse<Map<String, Object>> getProviders() {
        return ApiResponse.ok(Map.of(
            "providers", java.util.List.of(
                Map.of("name", "GitHub", "code", "github", "enabled", true),
                Map.of("name", "微信", "code", "wechat", "enabled", false)
            )
        ));
    }

    @GetMapping("/github/redirect")
    @Operation(summary = "GitHub登录重定向")
    public ApiResponse<Map<String, String>> githubLogin() {
        return ApiResponse.ok(Map.of(
            "redirectUrl", "/oauth2/authorization/github"
        ));
    }
}
