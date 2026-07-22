package com.flowbase.engine.collection.query;

import com.flowbase.engine.collection.domain.FieldType;
import org.springframework.stereotype.Component;

@Component
public class PostgreSqlDialect implements SqlDialect {
    @Override
    public String getJsonFieldPath(String fieldName, FieldType type) {
        if (type.equals(FieldType.NUMBER)) return "CAST(data ->> '" + fieldName + "' AS NUMERIC)";
        if (type.equals(FieldType.BOOLEAN)) return "CAST(data ->> '" + fieldName + "' AS BOOLEAN)";
        return "(data ->> '" + fieldName + "')";
    }
}
