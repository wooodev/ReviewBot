package com.reviewbot.auth.oauth2;

import com.reviewbot.auth.jwt.JwtTokenProvider;
import com.reviewbot.auth.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String githubId = String.valueOf(oAuth2User.getAttribute("id"));

        String userId = userRepository.findByGithubId(githubId)
                .orElseThrow(() -> new IllegalStateException("User not found after OAuth2 login"))
                .getId()
                .toString();

        String accessToken  = jwtTokenProvider.createAccessToken(userId);
        String refreshToken = jwtTokenProvider.createRefreshToken(userId);

        redisTemplate.opsForValue().set(
                "refresh_token:" + userId,
                refreshToken,
                Duration.ofMillis(jwtTokenProvider.getRefreshTokenExpiry())
        );

        response.sendRedirect(frontendUrl + "/auth/callback?token=" + accessToken
                + "&refreshToken=" + refreshToken);
    }
}
