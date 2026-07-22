package com.flowbase.engine.collection.dto;

import com.flowbase.engine.collection.domain.CollectionField;
import com.flowbase.engine.collection.domain.FieldType;

public record CollectionFieldResponse(String id, String name, FieldType type, boolean required, boolean indexed,
                                      boolean searchable) {
    public static CollectionFieldResponse from(CollectionField collectionField) {
        return new CollectionFieldResponse(collectionField.id(), collectionField.name(), collectionField.type(),
                collectionField.required(), collectionField.indexed(), collectionField.searchable());
    }
}
