package com.flowbase.engine.collection.query;

import com.flowbase.engine.collection.domain.FieldType;

public interface SqlDialect {
    String getJsonFieldPath(String fieldName, FieldType type);
}
