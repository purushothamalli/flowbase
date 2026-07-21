package com.flowbase.engine.gateway.service;

import com.flowbase.engine.gateway.dto.RateLimitResult;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RateLimiterService {
    private final StringRedisTemplate redisTemplate;
    private DefaultRedisScript<List> rateLimitScript;
    
    @PostConstruct
    private void init() {
        this.rateLimitScript = new DefaultRedisScript<>();
        rateLimitScript.setLocation(new ClassPathResource("scripts/rate_limit.lua"));
        rateLimitScript.setResultType(List.class);
    }
    
    public RateLimitResult checkRateLimit(String tenantId, int capacity, int refillRate, int requested) {
        String key = "rate_limit:" + tenantId;
        long now = Instant.now()
                          .getEpochSecond();
        List<Long> result = this.redisTemplate.execute(this.rateLimitScript, Collections.singletonList(key),
                String.valueOf(capacity), String.valueOf(refillRate), String.valueOf(now), String.valueOf(requested));
        boolean allowed = result != null && !result.isEmpty() && result.get(0) == 1L;
        long remaining = (result != null && result.size() > 1) ? result.get(1) : 0L;
        return new RateLimitResult(allowed, remaining, capacity);
    }
}
