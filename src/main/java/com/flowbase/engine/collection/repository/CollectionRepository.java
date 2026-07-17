package com.flowbase.engine.collection.repository;

import com.flowbase.engine.collection.domain.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollectionRepository extends JpaRepository<Collection, String> {}
