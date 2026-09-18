package com.ricozknow.common;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Fixed-window "at most N requests per window" limiter, backed by Redis
 * INCR + EXPIRE (same primitive LoginRateLimiter uses, but different
 * semantics on purpose: LoginRateLimiter counts *failures* and resets on
 * success — appropriate for brute-force protection. This counts *every*
 * call regardless of outcome — appropriate for general request throttling
 * on search and asset upload, where the point isn't "stop guessing," it's
 * "stop hammering."
 *
 * Deliberately not unified into one shared abstraction with LoginRateLimiter:
 * forcing those two semantics through the same API would make both harder
 * to read for what they actually do.
 */
@Component
@RequiredArgsConstructor
public class RateLimiter {

    private final StringRedisTemplate redisTemplate;

    /**
     * @return true if this call is within the limit (and has been counted
     *         against it); false if the caller should be rejected (429).
     */
    public boolean allow(String key, int maxRequests, Duration window) {
        String redisKey = "rl:" + key;
        Long count = redisTemplate.opsForValue().increment(redisKey);
        if (count != null && count == 1L) {
            redisTemplate.expire(redisKey, window);
        }
        return count != null && count <= maxRequests;
    }
}
