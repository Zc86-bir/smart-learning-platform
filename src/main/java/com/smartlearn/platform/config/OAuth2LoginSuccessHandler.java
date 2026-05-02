package com.smartlearn.platform.config;

import com.smartlearn.platform.entity.OAuth2UserBinding;
import com.smartlearn.platform.entity.Role;
import com.smartlearn.platform.entity.User;
import com.smartlearn.platform.entity.UserRole;
import com.smartlearn.platform.mapper.OAuth2UserBindingMapper;
import com.smartlearn.platform.mapper.RoleMapper;
import com.smartlearn.platform.mapper.UserMapper;
import com.smartlearn.platform.mapper.UserRoleMapper;
import com.smartlearn.platform.util.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final OAuth2UserBindingMapper oauth2BindingMapper;

    public OAuth2LoginSuccessHandler(JwtUtil jwtUtil, UserMapper userMapper, RoleMapper roleMapper,
                                     UserRoleMapper userRoleMapper, OAuth2UserBindingMapper oauth2BindingMapper) {
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.oauth2BindingMapper = oauth2BindingMapper;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
        String provider = oauth2Token.getAuthorizedClientRegistrationId();
        Map<String, Object> attributes = oauth2Token.getPrincipal().getAttributes();

        // Get provider-specific user ID
        String providerUserId = switch (provider) {
            case "github" -> String.valueOf(attributes.get("id"));
            case "wechat" -> String.valueOf(attributes.get("openid"));
            default -> throw new IllegalArgumentException("Unsupported provider: " + provider);
        };

        // Check if OAuth2 binding exists
        OAuth2UserBinding binding = oauth2BindingMapper.selectByProviderAndProviderUserId(provider, providerUserId);

        User user;
        if (binding == null) {
            // First time login via OAuth2, create new user
            user = new User();
            String username = "oauth_" + provider + "_" + System.currentTimeMillis();
            user.setUsername(username);
            user.setNickname((String) attributes.getOrDefault("name", username));
            user.setStatus(1);
            userMapper.insert(user);

            // Create OAuth2 binding
            OAuth2UserBinding newBinding = new OAuth2UserBinding();
            newBinding.setUserId(user.getId());
            newBinding.setProvider(provider);
            newBinding.setProviderUserId(providerUserId);
            oauth2BindingMapper.insert(newBinding);

            // Assign default STUDENT role
            Role studentRole = roleMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Role>()
                .eq(Role::getCode, "STUDENT")
            );
            if (studentRole != null) {
                UserRole ur = new UserRole();
                ur.setUserId(user.getId());
                ur.setRoleId(studentRole.getId());
                userRoleMapper.insert(ur);
            }
        } else {
            user = userMapper.selectById(binding.getUserId());
        }

        // Get roles from RBAC
        List<Role> roles = roleMapper.selectByUserId(user.getId());
        List<String> roleCodes = roles.stream()
            .map(Role::getCode)
            .toList();

        String primaryRole = roleCodes.isEmpty() ? "STUDENT" : roleCodes.get(0);

        // Generate JWT token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), primaryRole, roleCodes);

        // Redirect to frontend with token
        String redirectUrl = "/#/sso/callback?token=" + token + "&username=" + user.getUsername() + "&roles=" + String.join(",", roleCodes);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
