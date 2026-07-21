package com.flowbase.engine.storage.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigInteger;
import java.time.Instant;

@Entity
@Table(name = "FILE_METADATA")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(fluent = true)
public class FileMetadata {
    @Id
    @Column(name = "ID")
    private String id;
    @Column(name = "TENANT_ID")
    private String tenantId;
    @Column(name = "UPLOADER_ID")
    private String uploaderId;
    @Column(name = "FILENAME")
    private String filename;
    @Column(name = "CONTENT_TYPE")
    private String contentType;
    @Column(name = "SIZE_BYTES")
    private Long sizeBytes;
    @Column(name = "STORAGE_KEY")
    private String storageKey;
    @Column(name = "CREATED_AT")
    private Instant createdAt;
}
