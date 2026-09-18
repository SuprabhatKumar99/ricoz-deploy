package com.ricozknow.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Fixed-window login rate limiter keyed by tenant + email + client IP, backed by Redis.
 * Prevents brute-force credential guessing (spec section 6: "Login rate limiting").
 */
@Component
@RequiredArgsConstructor
public class LoginRateLimiter {

    private final StringRedisTemplate redisTemplate;

    @Value("${ricozknow.security.login-rate-limit.max-attempts}")
    private int maxAttempts;

    @Value("${ricozknow.security.login-rate-limit.window-minutes}")
    private long windowMinutes;

    public boolean isBlocked(String tenantSlug, String email, String clientIp) {
        String key = rateLimitKey(tenantSlug, email, clientIp);
        String value = redisTemplate.opsForValue().get(key);
        return value != null && Integer.parseInt(value) >= maxAttempts;
    }

    public void recordFailure(String tenantSlug, String email, String clientIp) {
        String key = rateLimitKey(tenantSlug, email, clientIp);
        Long attempts = redisTemplate.opsForValue().increment(key);
        if (attempts != null && attempts == 1L) {
            redisTemplate.expire(key, Duration.ofMinutes(windowMinutes));
        }
    }

    public void recordSuccess(String tenantSlug, String email, String clientIp) {
        redisTemplate.delete(rateLimitKey(tenantSlug, email, clientIp));
    }

    private String rateLimitKey(String tenantSlug, String email, String clientIp) {
        return "login-rl:%s:%s:%s".formatted(tenantSlug, email.toLowerCase(), clientIp);
    }
}
