package com.reviewbot.common.ratelimit;

import com.reviewbot.common.exception.RateLimitExceededException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis 기반 고정 윈도우(1시간) Rate Limiting.
 * 플랜별 AI API 호출 횟수를 제한한다.
 */
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;

    public void checkAndConsume(String userId, String plan) {
        String key = "rate_limit:" + userId;
        Long count = redisTemplate.opsForValue().increment(key);

        if (count != null && count == 1) {
            redisTemplate.expire(key, Duration.ofHours(1));
        }

        int limit = limitForPlan(plan);
        if (limit > 0 && count != null && count > limit) {
            throw new RateLimitExceededException(
                    "시간당 리뷰 한도를 초과했습니다. 플랜: %s, 한도: %d/시간".formatted(plan, limit));
        }
    }

    private int limitForPlan(String plan) {
        return switch (plan.toLowerCase()) {
            case "pro"  -> 200;
            case "team" -> 1000;
            default     -> 20;   // free
        };
    }
}
