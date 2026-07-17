package com.flowbase.engine.auth.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistService {
    private final Map<String, Instant> blacklistedTokens = new ConcurrentHashMap<>();
    
    public void revoke(String jti, Instant expiresAt) {
        this.blacklistedTokens.put(jti, expiresAt);
    }
    
    public boolean isRevoked(String jti) {
        return this.blacklistedTokens.containsKey(jti);
    }
    
    @Scheduled(cron = "0 */5 * * * *")
    private void evictExpiredKeys() {
        this.blacklistedTokens.entrySet().removeIf(entry -> entry.getValue().isBefore(Instant.now()));
    }
}
