package com.smartlearn.platform.controller;

import com.smartlearn.platform.dto.ApiResponse;
import com.smartlearn.platform.entity.Role;
import com.smartlearn.platform.entity.User;
import com.smartlearn.platform.entity.UserRole;
import com.smartlearn.platform.exception.BizException;
import com.smartlearn.platform.mapper.RoleMapper;
import com.smartlearn.platform.mapper.UserMapper;
import com.smartlearn.platform.mapper.UserRoleMapper;
import com.smartlearn.platform.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证", description = "登录接口")
public class AuthController {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthController(UserMapper userMapper, RoleMapper roleMapper,
                          UserRoleMapper userRoleMapper, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
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

        // 通过 RBAC 获取用户角色
        List<Role> roles = roleMapper.selectByUserId(user.getId());
        List<String> roleCodes = roles.stream()
            .map(Role::getCode)
            .collect(Collectors.toList());

        // 取第一个角色作为主要角色（向后兼容）
        String primaryRole = roleCodes.isEmpty() ? "STUDENT" : roleCodes.get(0);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), primaryRole, roleCodes);

        return ApiResponse.ok(Map.of(
            "id", user.getId(),
            "username", user.getUsername(),
            "nickname", user.getNickname(),
            "roles", roleCodes,
            "role", primaryRole,
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
        newUser.setStatus(1);

        userMapper.insert(newUser);

        // 默认分配学生角色
        Role studentRole = roleMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Role>()
            .eq(Role::getCode, "STUDENT")
        );
        if (studentRole != null) {
            UserRole ur = new UserRole();
            ur.setUserId(newUser.getId());
            ur.setRoleId(studentRole.getId());
            userRoleMapper.insert(ur);
        }

        String token = jwtUtil.generateToken(newUser.getId(), newUser.getUsername(), "STUDENT", List.of("STUDENT"));

        return ApiResponse.ok(Map.of(
            "id", newUser.getId(),
            "username", newUser.getUsername(),
            "nickname", newUser.getNickname(),
            "roles", List.of("STUDENT"),
            "role", "STUDENT",
            "token", token
        ));
    }
}
