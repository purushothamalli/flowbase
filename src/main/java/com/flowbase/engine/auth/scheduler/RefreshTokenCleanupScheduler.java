package com.flowbase.engine.auth.scheduler;

import com.flowbase.engine.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@Slf4j
@EnableScheduling
@RequiredArgsConstructor
public class RefreshTokenCleanupScheduler {
    private final RefreshTokenRepository refreshTokenRepository;
    
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanExpiredTokens() {
        int deletedCount = this.refreshTokenRepository.deleteByExpiresAtBefore(Instant.now());
        log.info("Daily cleanup complete. Deleted {} expired tokens.", deletedCount);
    }
}
