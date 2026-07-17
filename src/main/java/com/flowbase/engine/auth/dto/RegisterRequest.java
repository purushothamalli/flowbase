package com.flowbase.engine.auth.dto;

import com.flowbase.engine.auth.domain.UserRole;

public record RegisterRequest(String email, String password, UserRole role) {}
