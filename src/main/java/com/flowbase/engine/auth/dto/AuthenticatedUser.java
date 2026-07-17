package com.flowbase.engine.auth.dto;

import com.flowbase.engine.auth.domain.UserRole;

import java.time.Instant;

public record AuthenticatedUser(String id, String email, UserRole role, String jti, Instant expiresAt) {}
