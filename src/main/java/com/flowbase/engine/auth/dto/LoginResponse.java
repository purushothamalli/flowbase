package com.flowbase.engine.auth.dto;

public record LoginResponse(String accessToken, String tokenType, long expiresInSeconds) {}
