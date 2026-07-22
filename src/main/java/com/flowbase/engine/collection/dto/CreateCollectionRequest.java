package com.flowbase.engine.collection.dto;

import java.util.List;

public record CreateCollectionRequest(String name, String readRule, String writeRule, long cacheTtlSeconds,
                                      long lockTtlSeconds, List<CollectionFieldRequest> fields) {}
