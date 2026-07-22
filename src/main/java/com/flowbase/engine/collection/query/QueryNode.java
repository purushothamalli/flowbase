package com.flowbase.engine.collection.query;

import com.flowbase.engine.collection.domain.Collection;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public interface QueryNode {
    String compile(
            SqlDialect dialect,
            Map<FilterOperator, QueryCompiler.OperatorCompiler> compilers,
            Map<String, Object> params,
            Collection collection,
            AtomicInteger paramIndex
    );
}
