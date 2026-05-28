package com.reviewbot.auth.service;

import com.reviewbot.auth.dto.RefreshRequest;
import com.reviewbot.auth.dto.TokenResponse;
import com.reviewbot.auth.dto.UserResponse;
import com.reviewbot.auth.jwt.JwtTokenProvider;
import com.reviewbot.auth.repository.UserRepository;
import com.reviewbot.common.exception.ResourceNotFoundException;
import com.reviewbot.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;

    @Transactional(readOnly = true)
    public UserResponse getMe(String userId) {
        return userRepository.findById(UUID.fromString(userId))
                .map(UserResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public TokenResponse refresh(RefreshRequest request) {
        String token = request.refreshToken();

        if (!jwtTokenProvider.isValid(token) || !"refresh".equals(jwtTokenProvider.getTokenType(token))) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        String userId = jwtTokenProvider.getUserId(token);
        String stored = redisTemplate.opsForValue().get("refresh_token:" + userId);

        if (!token.equals(stored)) {
            throw new UnauthorizedException("Refresh token has been revoked");
        }

        String newAccessToken  = jwtTokenProvider.createAccessToken(userId);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(userId);

        redisTemplate.opsForValue().set(
                "refresh_token:" + userId,
                newRefreshToken,
                Duration.ofMillis(jwtTokenProvider.getRefreshTokenExpiry())
        );

        return new TokenResponse(newAccessToken, newRefreshToken, jwtTokenProvider.getAccessTokenExpiry());
    }

    public void logout(String userId) {
        redisTemplate.delete("refresh_token:" + userId);
    }
}
