package com.flowbase.engine.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {
    private final StringRedisTemplate redisTemplate;
    private static final String BLACKLIST_PREFIX = "blacklist:token:";
    
    public void revoke(String jti, Instant expiresAt) {
        long ttlSeconds = Duration.between(Instant.now(), expiresAt)
                                  .getSeconds();
        if (ttlSeconds > 0) {
            this.redisTemplate.opsForValue()
                              .set(BLACKLIST_PREFIX + jti, "revoked", ttlSeconds);
        }
    }
    
    public boolean isRevoked(String jti) {
        return Boolean.TRUE.equals(this.redisTemplate.hasKey(BLACKLIST_PREFIX + jti));
    }
}
