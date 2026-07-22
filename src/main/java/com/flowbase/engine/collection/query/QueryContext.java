package com.flowbase.engine.collection.query;

import java.util.List;

public record QueryContext(QueryNode rootNode, String sortBy, int limit, int offset) {}
