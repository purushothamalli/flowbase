package com.flowbase.engine.collection.dto;

import com.flowbase.engine.collection.domain.CollectionDocument;

import java.time.Instant;
import java.util.Map;

public record CollectionDocumentResponse(String id,
                                         String collectionId, Map<String, Object> data, Instant createdAt,
                                         Instant updatedAt) {
    public static CollectionDocumentResponse from(CollectionDocument collectionDocument) {
        return new CollectionDocumentResponse(
                collectionDocument.id(),
                collectionDocument.collectionId(),
                collectionDocument.data(),
                collectionDocument.createdAt(),
                collectionDocument.updatedAt()
        );
    }
}
