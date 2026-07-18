package com.flowbase.engine.collection.repository;

import com.flowbase.engine.collection.domain.Collection;
import com.flowbase.engine.collection.domain.CollectionRepository;
import com.flowbase.engine.collection.repository.jpa.SpringDataCollectionRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@AllArgsConstructor
public class JpaCollectionRepositoryAdapter implements CollectionRepository {
    private final SpringDataCollectionRepository collectionRepository;
    
    @Override
    public Optional<Collection> findById(String id) {
        return this.collectionRepository.findById(id);
    }
    
    @Override
    public Collection save(Collection collection) {
        return this.collectionRepository.save(collection);
    }
    
    @Override
    public List<Collection> findByTenantId(String tenantId) {
        return this.collectionRepository.findByTenantId(tenantId);
    }
    
    @Override
    public Optional<Collection> findByTenantIdAndName(String tenantId, String name) {
        return this.collectionRepository.findByTenantIdAndName(tenantId, name);
    }
    
    @Override
    public void delete(String id) {
        this.collectionRepository.deleteById(id);
    }
}
