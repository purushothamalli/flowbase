package com.flowbase.engine.auth.dto;

public record AuthResult(String accessToken, String refreshToken, long expiresInSeconds) {
}
