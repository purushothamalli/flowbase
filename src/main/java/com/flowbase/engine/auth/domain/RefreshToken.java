package com.flowbase.engine.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.Instant;

@Entity
@Table(name = "REFRESH_TOKENS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(fluent = true)
public class RefreshToken {
    @Id
    @Column(name = "TOKEN")
    private String token;
    @Column(name = "USER_ID")
    private String userId;
    @Column(name = "TENANT_ID")
    private String tenantId;
    @Column(name = "EXPIRES_AT")
    private Instant expiresAt;
    @Column(name = "REVOKED")
    private boolean revoked;
}
