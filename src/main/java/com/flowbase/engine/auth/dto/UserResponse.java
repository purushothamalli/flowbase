package com.flowbase.engine.auth.dto;

import com.flowbase.engine.auth.domain.UserRole;

public record UserResponse(String id, String email, UserRole role) {}
