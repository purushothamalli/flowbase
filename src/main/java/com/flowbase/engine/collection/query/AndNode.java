package com.flowbase.engine.collection.query;

import com.flowbase.engine.collection.domain.Collection;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class AndNode implements QueryNode {
    private final List<QueryNode> children;
    
    @Override
    public String compile(SqlDialect dialect, Map<FilterOperator, QueryCompiler.OperatorCompiler> compilers, Map<String, Object> params, Collection collection, AtomicInteger paramIndex) {
        if (this.children.isEmpty()) return "1=1";
        return children.stream()
                       .map(c -> c.compile(dialect, compilers, params, collection, paramIndex))
                       .collect(Collectors.joining(" AND ", "(", ")"));
    }
}
