package com.reviewbot.common.ratelimit;

import com.reviewbot.common.exception.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitService — Redis 슬라이딩 윈도우")
class RateLimitServiceTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOps;
    @InjectMocks RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    @DisplayName("첫 번째 호출 시 TTL 1시간 설정")
    void firstCall_setsExpiry() {
        when(valueOps.increment("rate_limit:u1")).thenReturn(1L);

        rateLimitService.checkAndConsume("u1", "free");

        verify(redisTemplate).expire(eq("rate_limit:u1"), eq(Duration.ofHours(1)));
    }

    @Test
    @DisplayName("Free 플랜 — 한도 내 호출 성공")
    void free_withinLimit_success() {
        when(valueOps.increment(any())).thenReturn(20L);

        assertThatCode(() -> rateLimitService.checkAndConsume("u1", "free"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Free 플랜 — 한도(20) 초과 시 예외")
    void free_exceedsLimit_throws() {
        when(valueOps.increment(any())).thenReturn(21L);

        assertThatThrownBy(() -> rateLimitService.checkAndConsume("u1", "free"))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("한도");
    }

    @Test
    @DisplayName("Pro 플랜 — 200 이하는 성공")
    void pro_withinLimit_success() {
        when(valueOps.increment(any())).thenReturn(200L);

        assertThatCode(() -> rateLimitService.checkAndConsume("u1", "pro"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Pro 플랜 — 201 초과 시 예외")
    void pro_exceedsLimit_throws() {
        when(valueOps.increment(any())).thenReturn(201L);

        assertThatThrownBy(() -> rateLimitService.checkAndConsume("u1", "pro"))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    @DisplayName("Team 플랜 — 1000 이하는 성공")
    void team_withinLimit_success() {
        when(valueOps.increment(any())).thenReturn(1000L);

        assertThatCode(() -> rateLimitService.checkAndConsume("u1", "team"))
                .doesNotThrowAnyException();
    }
}
