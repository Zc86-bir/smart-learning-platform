package com.smartlearn.platform.controller;

import com.smartlearn.platform.dto.ApiResponse;
import com.smartlearn.platform.exception.BizException;
import com.smartlearn.platform.entity.User;
import com.smartlearn.platform.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证", description = "登录接口")
public class AuthController {

    private final UserMapper userMapper;

    public AuthController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @PostMapping("/login")
    @Operation(summary = "用户名密码登录")
    public ApiResponse<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        if (username == null || username.isBlank()) {
            throw new BizException("请输入用户名");
        }

        User user = userMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
            .eq(User::getUsername, username.trim())
            .eq(User::getDeleted, 0)
        );
        if (user == null) {
            throw new BizException(401, "用户不存在");
        }

        return ApiResponse.ok(Map.of(
            "id", user.getId(),
            "username", user.getUsername(),
            "nickname", user.getNickname(),
            "role", user.getRole()
        ));
    }
}
