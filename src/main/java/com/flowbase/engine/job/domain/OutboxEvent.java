package com.flowbase.engine.job.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.Instant;

@Entity
@Table(name = "OUTBOX_EVENT")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(fluent = true)
public class OutboxEvent {
    @Id
    @Column(name = "ID")
    private String id;
    @Column(name = "TENANT_ID")
    private String tenantId;
    @Column(name = "EVENT_TYPE")
    private String eventType;
    @Column(name = "PAYLOAD")
    private String payload;
    @Column(name = "STATUS")
    @Enumerated(EnumType.STRING)
    private OutboxStatus status;
    @Column(name = "RETRY_COUNT")
    private int retryCount;
    @Column(name = "MAX_RETRIES")
    private int maxRetries;
    @Column(name = "ERROR_MESSAGE")
    private String errorMessage;
    @Column(name = "LEASED_UNTIL")
    private Instant leasedUntil;
    @Column(name = "CREATED_AT")
    private Instant createdAt;
    @Column(name = "UPDATED_AT")
    private Instant updatedAt;
}
