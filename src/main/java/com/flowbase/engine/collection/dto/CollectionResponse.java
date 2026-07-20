package com.flowbase.engine.collection.dto;

import java.util.List;

public record CollectionResponse(String id, String name, String readRule, String writeRule,
                                 List<CollectionFieldResponse> fields) {}
