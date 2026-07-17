package com.flowbase.engine.collection.service;

import com.flowbase.engine.collection.domain.CollectionDocument;

import java.util.Map;

public interface CollectionDataService {
    CollectionDocument insertDocument(String collectionId, Map<String, Object> Payload);
}
