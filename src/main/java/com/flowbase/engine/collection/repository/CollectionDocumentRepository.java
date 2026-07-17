package com.flowbase.engine.collection.repository;

import com.flowbase.engine.collection.domain.CollectionDocument;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollectionDocumentRepository extends JpaRepository<CollectionDocument, String> {}
