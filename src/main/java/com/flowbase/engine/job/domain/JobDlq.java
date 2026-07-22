package com.flowbase.engine.job.domain;

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
@Table(name = "JOB_DLQ")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(fluent = true)
public class JobDlq {
    @Id
    @Column(name = "ID")
    private String id;

    @Column(name = "EVENT_TYPE")
    private String eventType;

    @Column(name = "TENANT_ID")
    private String tenantId;

    @Column(name = "PAYLOAD")
    private String payload;

    @Column(name = "ERROR_MESSAGE")
    private String errorMessage;

    @Column(name = "FAILED_AT")
    private Instant failedAt;
}
