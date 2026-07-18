package com.flowbase.engine.collection.dto;

import java.time.Instant;
import java.util.Map;

public record CollectionDocumentResponse(String id, String collectionId, Map<String, Object> data, Instant createdAt,
                                         Instant updatedAt) {
}
