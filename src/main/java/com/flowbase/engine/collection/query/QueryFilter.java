package com.flowbase.engine.collection.query;

public record QueryFilter(String field, FilterOperator operator, Object value) {}
