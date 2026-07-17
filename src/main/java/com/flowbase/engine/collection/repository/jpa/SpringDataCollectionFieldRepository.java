package com.flowbase.engine.collection.repository.jpa;

import com.flowbase.engine.collection.domain.CollectionField;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataCollectionFieldRepository extends JpaRepository<CollectionField, String> {}