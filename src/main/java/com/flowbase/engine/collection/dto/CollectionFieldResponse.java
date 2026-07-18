package com.flowbase.engine.collection.dto;

import com.flowbase.engine.collection.domain.FieldType;

public record CollectionFieldResponse(String id, String name, FieldType type, boolean required) {}
