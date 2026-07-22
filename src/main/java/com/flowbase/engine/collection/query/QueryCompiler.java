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
import java.util.concurrent.atomic.AtomicInteger;

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
        AtomicInteger paramIndex = new AtomicInteger(0);
        String astSql = queryContext.rootNode().compile(sqlDialect,this.operatorCompilers,params,collection,paramIndex);
        sql.append(" AND ").append(astSql);
        return new CompiledQuery(sql.toString(), params);
    }
}
