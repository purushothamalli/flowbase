package com.flowbase.engine.collection.query;

import java.util.Map;

public record CompiledQuery(String sql, Map<String, Object> params) {}
