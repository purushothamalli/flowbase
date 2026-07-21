package com.flowbase.engine.storage.dto;

import com.flowbase.engine.storage.domain.FileMetadata;

import java.time.Instant;

public record FileMetadataResponse(String id, String tenantId, String uploaderId, String filename, String contentType,
                                   Long sizeBytes, Instant createdAt) {
    public static FileMetadataResponse from(FileMetadata metadata) {
        return new FileMetadataResponse(metadata.id(), metadata.tenantId(), metadata.uploaderId(), metadata.filename(), metadata.contentType(), metadata.sizeBytes(), metadata.createdAt());
    }
}
