package com.flowbase.engine.collection.repository.jpa;

import com.flowbase.engine.collection.domain.CollectionDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataCollectionDocumentRepository extends JpaRepository<CollectionDocument, String> {
    List<CollectionDocument> findCollectionDocumentsByCollectionId(String collectionId);
}
