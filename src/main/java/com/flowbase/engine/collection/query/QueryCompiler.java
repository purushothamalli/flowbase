package com.flowbase.engine.collection.query;

import com.flowbase.engine.collection.domain.Collection;
import com.flowbase.engine.collection.domain.CollectionField;
import com.flowbase.engine.collection.domain.FieldType;
import com.flowbase.engine.collection.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class QueryCompiler {
    private final SqlDialect sqlDialect;
    private final Map<FilterOperator, OperatorCompiler> operatorCompilers = new EnumMap<>(FilterOperator.class);
    
    @FunctionalInterface
    public interface OperatorCompiler {
        String compile(String sqlPath, String paramName, String fieldName);
    }
    
    public QueryCompiler(SqlDialect sqlDialect) {
        this.sqlDialect = sqlDialect; this.initializeOperatorCompilers();
    }
    
    private void initializeOperatorCompilers() {
        operatorCompilers.put(FilterOperator.EQ, (sqlPath, paramName, fName) -> sqlPath + " = :" + paramName);
        operatorCompilers.put(FilterOperator.NEQ, (sqlPath, paramName, fName) -> sqlPath + " != :" + paramName);
        operatorCompilers.put(FilterOperator.GT, (sqlPath, paramName, fName) -> sqlPath + " > :" + paramName);
        operatorCompilers.put(FilterOperator.GTE, (sqlPath, paramName, fName) -> sqlPath + " >= :" + paramName);
        operatorCompilers.put(FilterOperator.LT, (sqlPath, paramName, fName) -> sqlPath + " < :" + paramName);
        operatorCompilers.put(FilterOperator.LTE, (sqlPath, paramName, fName) -> sqlPath + " <= :" + paramName);
        operatorCompilers.put(FilterOperator.LIKE, (sqlPath, paramName, fName) -> sqlPath + " LIKE :" + paramName);
        operatorCompilers.put(FilterOperator.I_LIKE, (sqlPath, paramName, fName) -> sqlPath + " ILIKE :" + paramName);
        operatorCompilers.put(FilterOperator.STARTS_WITH, (sqlPath, paramName, fName) -> sqlPath + " ILIKE :" + paramName);
        operatorCompilers.put(FilterOperator.ENDS_WITH, (sqlPath, paramName, fName) -> sqlPath + " ILIKE :" + paramName);
        operatorCompilers.put(FilterOperator.CONTAINS, (sqlPath, paramName, fName) -> sqlPath + " ILIKE :" + paramName);
        operatorCompilers.put(FilterOperator.IN, (sqlPath, paramName, fName) -> sqlPath + " IN (:" + paramName + ")");
        operatorCompilers.put(FilterOperator.NOT_IN, (sqlPath, paramName, fName) -> sqlPath + " NOT IN (:" + paramName + ")");
        operatorCompilers.put(FilterOperator.IS_NULL, (sqlPath, paramName, fName) -> "(data ->> '" + fName + "') IS NULL");
        operatorCompilers.put(FilterOperator.IS_NOT_NULL, (sqlPath, paramName, fName) -> "(data ->> '" + fName + "') IS NOT NULL");
    }
    
    public CompiledQuery compile(Collection collection, QueryContext queryContext) {
        StringBuilder sql = new StringBuilder().append("collection_Id = :collectionId_const");
        Map<String, Object> params = new HashMap<>();
        params.put("collectionId_const", collection.id());
        if (queryContext.filters() == null || queryContext.filters().isEmpty())
            return new CompiledQuery(sql.toString(), params);
        for (int i = 0; i < queryContext.filters().size(); i++) {
            QueryFilter filter = queryContext.filters().get(i);
            CollectionField field = collection.fields()
                                              .stream()
                                              .filter(f -> f.name().equals(filter.field()))
                                              .findFirst()
                                              .orElseThrow(() -> new ValidationException("FIeld '" + filter.field() + "' not defined in collection schema"));
            String sqlPath = this.sqlDialect.getJsonFieldPath(field.name(), field.type());
            String parameterName = field.name() + "_" + i;
            OperatorCompiler compiler = this.operatorCompilers.get(filter.operator());
            if (compiler == null) throw new UnsupportedOperationException("Operator " + filter.operator() + " is not " +
                    "implemented");
            String sqlClause = compiler.compile(sqlPath,parameterName, field.name());
            String rawValue = String.valueOf(filter.value());
            if (filter.operator() == FilterOperator.IS_NULL || filter.operator() == FilterOperator.IS_NOT_NULL) {
                //no work! pass!
            } else if (filter.operator() == FilterOperator.IN || filter.operator() == FilterOperator.NOT_IN) {
                List<Object> valuesList = Arrays.stream(rawValue.split(","))
                                                .map(val -> this.castValue(val.trim(), field.type()))
                                                .toList(); params.put(parameterName, valuesList);
            } else if (filter.operator() == FilterOperator.STARTS_WITH) {
                params.put(parameterName, rawValue + "%");
            } else if (filter.operator() == FilterOperator.ENDS_WITH) {
                params.put(parameterName, "%" + rawValue);
            } else if (filter.operator() == FilterOperator.CONTAINS) {
                params.put(parameterName, "%" + rawValue + "%");
            } else {
                params.put(parameterName, this.castValue(rawValue, field.type()));
            } sql.append(" AND ").append(sqlClause);
        } return new CompiledQuery(sql.toString(), params);
    }
    
    private Object castValue(String value, FieldType type) {
        if (type == FieldType.NUMBER) return new BigDecimal(value);
        else if (type == FieldType.BOOLEAN) return Boolean.parseBoolean(value);
        else return value;
    }
}
