package com.flowbase.engine.collection.repository.jpa;

import com.flowbase.engine.collection.domain.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpringDataCollectionRepository extends JpaRepository<Collection, String> {
    
    List<Collection> findByTenantId(String tenantId);
    
    Optional<Collection> findByTenantIdAndName(String tenantId, String name);
}
