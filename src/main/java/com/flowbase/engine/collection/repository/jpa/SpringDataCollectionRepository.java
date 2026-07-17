package com.flowbase.engine.collection.repository.jpa;

import com.flowbase.engine.collection.domain.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataCollectionRepository extends JpaRepository<Collection, String> {}
