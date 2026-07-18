package com.flowbase.engine.collection.service;

import com.flowbase.engine.collection.dto.CollectionResponse;
import com.flowbase.engine.collection.dto.CreateCollectionRequest;

import java.util.List;

public interface CollectionService {
    CollectionResponse createCollection(CreateCollectionRequest request);
    
    List<CollectionResponse> listCollections(String tenantId);
    
    CollectionResponse getCollection(String id);
    
    void deleteCollection(String id);
}
