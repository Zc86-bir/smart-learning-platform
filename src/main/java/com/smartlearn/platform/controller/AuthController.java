package com.smartlearn.platform.controller;

import com.smartlearn.platform.dto.ApiResponse;
import com.smartlearn.platform.entity.User;
import com.smartlearn.platform.exception.BizException;
import com.smartlearn.platform.mapper.UserMapper;
import com.smartlearn.platform.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证", description = "登录接口")
public class AuthController {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthController(UserMapper userMapper, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    @Operation(summary = "用户名密码登录")
    public ApiResponse<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        if (username == null || username.isBlank()) {
            throw new BizException("请输入用户名");
        }
        if (password == null || password.isBlank()) {
            throw new BizException("请输入密码");
        }

        User user = userMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
            .eq(User::getUsername, username.trim())
            .eq(User::getDeleted, 0)
        );
        if (user == null) {
            throw new BizException(401, "用户名或密码错误");
        }

        if (user.getPassword() == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new BizException(401, "用户名或密码错误");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        return ApiResponse.ok(Map.of(
            "id", user.getId(),
            "username", user.getUsername(),
            "nickname", user.getNickname(),
            "role", user.getRole(),
            "token", token
        ));
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public ApiResponse<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String nickname = body.get("nickname");

        if (username == null || username.isBlank()) {
            throw new BizException("请输入用户名");
        }
        if (password == null || password.length() < 6) {
            throw new BizException("密码长度不能少于6位");
        }

        // Check if username already exists
        User existing = userMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
            .eq(User::getUsername, username.trim())
        );
        if (existing != null) {
            throw new BizException("用户名已存在");
        }

        // Create new user
        User newUser = new User();
        newUser.setUsername(username.trim());
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setNickname(nickname != null && !nickname.isBlank() ? nickname : username);
        newUser.setRole("STUDENT");

        userMapper.insert(newUser);

        String token = jwtUtil.generateToken(newUser.getId(), newUser.getUsername(), newUser.getRole());

        return ApiResponse.ok(Map.of(
            "id", newUser.getId(),
            "username", newUser.getUsername(),
            "nickname", newUser.getNickname(),
            "role", newUser.getRole(),
            "token", token
        ));
    }
}
