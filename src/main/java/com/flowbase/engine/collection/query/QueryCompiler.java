package com.flowbase.engine.collection.query;

import com.flowbase.engine.collection.domain.Collection;
import com.flowbase.engine.collection.domain.CollectionField;
import com.flowbase.engine.collection.domain.FieldType;
import com.flowbase.engine.collection.exception.ValidationException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class QueryCompiler {
    public CompiledQuery compile(Collection collection, QueryContext queryContext) {
        StringBuilder sql = new StringBuilder().append("collection_Id = :collectionId_const");
        Map<String, Object> params = new HashMap<>();
        params.put("collectionId_const", collection.id());
        if (queryContext.filters() == null || queryContext.filters().isEmpty())
            return new CompiledQuery(sql.toString(), params);
        for (int i = 0; i < queryContext.filters().size(); i++) {
            QueryFilter filter = queryContext.filters().get(i);
            CollectionField field = collection
                    .fields()
                    .stream()
                    .filter(f -> f.name().equals(filter.field()))
                    .findFirst()
                    .orElseThrow(() -> new ValidationException("FIeld '" + filter.field() + "' not defined in collection schema"));
            String sqlPath;
            if (field.type().equals(FieldType.NUMBER)) {
                sqlPath = "CAST( data ->> '" + field.name() + "' AS NUMERIC)";
            } else if (field.type().equals(FieldType.BOOLEAN)) {
                sqlPath = "CAST( data ->> '" + field.name() + "' AS BOOLEAN)";
            } else {
                sqlPath = "( data ->> '" + field.name() + "')";
            }
            String parameterName = field.name() + "_" + i;
            String sqlClause = "";
            switch (filter.operator()) {
                case EQ -> sqlClause = sqlPath + " = :" + parameterName;
                case NEQ -> sqlClause = sqlPath + " != :" + parameterName;
                case GT -> sqlClause = sqlPath + " > :" + parameterName;
                case GTE -> sqlClause = sqlPath + " >= :" + parameterName;
                case LT -> sqlClause = sqlPath + " < :" + parameterName;
                case LTE -> sqlClause = sqlPath + " <= :" + parameterName;
                case LIKE -> sqlClause = sqlPath + " LIKE :" + parameterName;
                case I_LIKE,
                     STARTS_WITH,
                     ENDS_WITH,
                     CONTAINS -> sqlClause = sqlPath + " ILIKE :" + parameterName;
                case IN -> sqlClause = sqlPath + " IN (:" + parameterName + ")";
                case NOT_IN -> sqlClause = sqlPath + " NOT IN (:" + parameterName + ")";
                case IS_NULL -> sqlClause = "(data ->> '" + field.name() + "') IS NULL";
                case IS_NOT_NULL -> sqlClause = "(data ->> '" + field.name() + "') IS NOT NULL";
            }
            String rawValue = String.valueOf(filter.value());
            if (filter.operator() == FilterOperator.IS_NULL || filter.operator() == FilterOperator.IS_NOT_NULL) {
                
            } else if (filter.operator() == FilterOperator.IN || filter.operator() == FilterOperator.NOT_IN) {
                List<Object> valuesList = Arrays.stream(rawValue.split(",")).map(val -> this.castValue(val.trim(),
                        field.type())).toList();
                params.put(parameterName, valuesList);
            } else if (filter.operator() == FilterOperator.STARTS_WITH) {
                params.put(parameterName, rawValue + "%");
            } else if (filter.operator() == FilterOperator.ENDS_WITH) {
                params.put(parameterName, "%" + rawValue);
            } else if (filter.operator() == FilterOperator.CONTAINS) {
                params.put(parameterName, "%" + rawValue + "%");
            } else {
                params.put(parameterName, this.castValue(rawValue, field.type()));
            }
            sql.append(" AND ").append(sqlClause);
        }
        return new CompiledQuery(sql.toString(), params);
    }
    
    private Object castValue(String value, FieldType type) {
        if (type == FieldType.NUMBER) return new BigDecimal(value);
        else if (type == FieldType.BOOLEAN) return Boolean.parseBoolean(value);
        else return value;
    }
}
