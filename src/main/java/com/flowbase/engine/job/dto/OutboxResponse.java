package com.flowbase.engine.job.dto;

import com.flowbase.engine.job.domain.OutboxEvent;
import com.flowbase.engine.job.domain.OutboxStatus;

import java.time.Instant;

public record OutboxResponse(String id, String tenantId, String eventType, String payload, OutboxStatus status,
                             int retryCount, int maxRetries, String errorMessage, Instant leasedUntil,
                             Instant createdAt, Instant updatedAt) {
    public static OutboxResponse from(OutboxEvent event) {
        return new OutboxResponse(event.id(), event.tenantId(), event.eventType(), event.payload(), event.status(), event.retryCount(), event.maxRetries(), event.errorMessage(), event.leasedUntil(), event.createdAt(), event.updatedAt());
    }
}
