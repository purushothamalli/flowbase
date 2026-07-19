package com.flowbase.engine.collection.query;

import java.util.List;

public record QueryContext(List<QueryFilter> filters, String sortBy, int limit, int offset) {}
