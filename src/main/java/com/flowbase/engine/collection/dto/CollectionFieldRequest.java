package com.flowbase.engine.collection.dto;

import com.flowbase.engine.collection.domain.FieldType;

public record CollectionFieldRequest(String name, FieldType type, boolean required,boolean indexed) {}
