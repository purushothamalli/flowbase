package com.flowbase.engine.gateway.dto;

public record RateLimitResult(boolean allowed, long remainingTokens, long capacity) {}
