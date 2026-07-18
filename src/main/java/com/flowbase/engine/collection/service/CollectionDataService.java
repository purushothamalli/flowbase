package com.flowbase.engine.collection.service;

import com.flowbase.engine.collection.domain.CollectionDocument;

import java.util.List;
import java.util.Map;

public interface CollectionDataService {
    CollectionDocument insertDocument(String collectionId, Map<String, Object> Payload);
    
    CollectionDocument getDocument(String collectionId, String id);
    
    List<CollectionDocument> findDocumentsByCollection(String collectionId);
    
    CollectionDocument updateDocument(String collectionId, String id, Map<String, Object> payload);
    
    void deleteDocument(String collectionId, String id);
}
