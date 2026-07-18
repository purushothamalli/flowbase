package com.flowbase.engine.collection.domain;

import java.util.List;
import java.util.Optional;

public interface CollectionRepository {
    Optional<Collection> findById(String id);
    
    Collection save(Collection collection);
    
    List<Collection> findByTenantId(String tenantId);
    
    Optional<Collection> findByTenantIdAndName(String tenantId, String name);
    
    void delete(String id);
}
