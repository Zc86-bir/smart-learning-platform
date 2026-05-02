package com.smartlearn.platform.config;

import com.smartlearn.platform.entity.OAuth2UserBinding;
import com.smartlearn.platform.entity.User;
import com.smartlearn.platform.mapper.OAuth2UserBindingMapper;
import com.smartlearn.platform.mapper.UserMapper;
import com.smartlearn.platform.util.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final OAuth2UserBindingMapper oauth2BindingMapper;

    public OAuth2LoginSuccessHandler(JwtUtil jwtUtil, UserMapper userMapper, OAuth2UserBindingMapper oauth2BindingMapper) {
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
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
            user.setRole("STUDENT");
            userMapper.insert(user);

            // Create OAuth2 binding
            OAuth2UserBinding newBinding = new OAuth2UserBinding();
            newBinding.setUserId(user.getId());
            newBinding.setProvider(provider);
            newBinding.setProviderUserId(providerUserId);
            oauth2BindingMapper.insert(newBinding);
        } else {
            user = userMapper.selectById(binding.getUserId());
        }

        // Generate JWT token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        // Redirect to frontend with token
        String redirectUrl = "/#/sso/callback?token=" + token + "&username=" + user.getUsername() + "&role=" + user.getRole();
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
