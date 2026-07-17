package com.flowbase.engine.collection.domain;

import java.util.List;
import java.util.Optional;

public interface CollectionDocumentRepository {
    Optional<CollectionDocument> findById(String id);
    CollectionDocument save(CollectionDocument collectionDocument);
    void delete(CollectionDocument collectionDocument);
    List<CollectionDocument> findByCollectionId(String id);
}
