package com.flowbase.engine.collection.dto;

import java.util.List;

public record CreateCollectionRequest(String name, String readRule, String writeRule,
                                      List<CollectionFieldRequest> fields) {}
