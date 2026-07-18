package com.flowbase.engine.collection.dto;

import java.util.List;

public record CollectionResponse(String id, String name, List<CollectionFieldResponse> fields) {}
