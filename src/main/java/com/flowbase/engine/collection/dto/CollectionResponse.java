package com.flowbase.engine.collection.dto;

import com.flowbase.engine.collection.domain.Collection;

import java.util.List;

public record CollectionResponse(String id, String name, String readRule, String writeRule, long cacheTtlSeconds,
                                 long lockTtlSeconds, List<CollectionFieldResponse> fields) {
    public static CollectionResponse from(Collection collection) {
        return new CollectionResponse(
                collection.id(),
                collection.name(),
                collection.readRule(),
                collection.writeRule(),
                collection.cacheTtlSeconds(), collection.lockTtlSeconds(),
                collection.fields()
                          .stream()
                          .map(CollectionFieldResponse::from)
                          .toList()
        );
    }
}
