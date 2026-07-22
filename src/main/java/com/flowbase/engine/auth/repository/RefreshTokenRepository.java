package com.flowbase.engine.auth.repository;

import com.flowbase.engine.auth.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    void deleteAllByUserId(String userId);
    
    int deleteByExpiresAtBefore(Instant expiresAt);
}
