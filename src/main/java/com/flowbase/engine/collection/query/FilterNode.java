package com.flowbase.engine.collection.query;

import com.flowbase.engine.collection.domain.Collection;
import com.flowbase.engine.collection.domain.CollectionField;
import com.flowbase.engine.collection.domain.FieldType;
import com.flowbase.engine.collection.exception.ValidationException;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
public class FilterNode implements QueryNode {
    private final QueryFilter filter;
    
    @Override
    public String compile(SqlDialect dialect, Map<FilterOperator, QueryCompiler.OperatorCompiler> compilers,
                          Map<String, Object> params, Collection collection, AtomicInteger paramIndex) {
        CollectionField field =
                collection.fields()
                          .stream()
                          .filter(f -> f.name().equals(this.filter.field()))
                          .findFirst()
                          .orElseThrow(() ->
                                  new ValidationException("Field " + this.filter.field() + " not defined in collection schema!")
                          );
        String sqlPath = dialect.getJsonFieldPath(field.name(), field.type());
        String paramName = field.name() + "_" + paramIndex.getAndIncrement();
        QueryCompiler.OperatorCompiler compiler = compilers.get(filter.operator());
        if (compiler == null)
            throw new UnsupportedOperationException("Operator " + filter.operator() + "is not implemented!");
        String sqlClause = compiler.compile(sqlPath, paramName, field.name());
        String rawValue = String.valueOf(filter.value());
        if (filter.operator() == FilterOperator.IS_NULL || filter.operator() == FilterOperator.IS_NOT_NULL) {
            // No binding parameter needed
        } else if (filter.operator() == FilterOperator.IN || filter.operator() == FilterOperator.NOT_IN) {
            List<Object> valuesList = Arrays.stream(rawValue.split(","))
                                            .map(val -> this.castValue(val.trim(), field.type()))
                                            .toList();
            params.put(paramName, valuesList);
        } else if (filter.operator() == FilterOperator.STARTS_WITH) {
            params.put(paramName, rawValue + "%");
        } else if (filter.operator() == FilterOperator.ENDS_WITH) {
            params.put(paramName, "%" + rawValue);
        } else if (filter.operator() == FilterOperator.CONTAINS) {
            params.put(paramName, "%" + rawValue + "%");
        } else {
            params.put(paramName, this.castValue(rawValue, field.type()));
        }
        return sqlClause;
    }
    
    private Object castValue(String value, FieldType type) {
        if (type == FieldType.NUMBER) return new BigDecimal(value);
        if (type == FieldType.BOOLEAN) return Boolean.parseBoolean(value);
        return value;
    }
}
