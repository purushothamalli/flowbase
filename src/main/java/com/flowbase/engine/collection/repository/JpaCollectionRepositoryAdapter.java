package com.flowbase.engine.collection.repository;

import com.flowbase.engine.collection.domain.Collection;
import com.flowbase.engine.collection.domain.CollectionRepository;
import com.flowbase.engine.collection.repository.jpa.SpringDataCollectionRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

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
}
