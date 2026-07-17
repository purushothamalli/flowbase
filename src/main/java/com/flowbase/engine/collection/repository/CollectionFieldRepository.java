package com.flowbase.engine.collection.repository;

import com.flowbase.engine.collection.domain.CollectionField;
import org.springframework.data.jpa.repository.JpaRepository;

interface CollectionFieldRepository extends JpaRepository<CollectionField, String> {}