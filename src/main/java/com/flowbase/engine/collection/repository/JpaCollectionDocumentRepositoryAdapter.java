package com.flowbase.engine.collection.repository;

import com.flowbase.engine.collection.domain.Collection;
import com.flowbase.engine.collection.domain.CollectionDocument;
import com.flowbase.engine.collection.domain.CollectionDocumentRepository;
import com.flowbase.engine.collection.repository.jpa.SpringDataCollectionDocumentRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@AllArgsConstructor
public class JpaCollectionDocumentRepositoryAdapter implements CollectionDocumentRepository {
    private final SpringDataCollectionDocumentRepository collectionDocumentRepository;
    
    @Override
    public Optional<CollectionDocument> findById(String id) {
        return this.collectionDocumentRepository.findById(id);
    }
    
    @Override
    public CollectionDocument save(CollectionDocument collectionDocument) {
        return this.collectionDocumentRepository.save(collectionDocument);
    }
    
    @Override
    public void delete(CollectionDocument collectionDocument) {
        this.collectionDocumentRepository.delete(collectionDocument);
    }
    
    @Override
    public List<CollectionDocument> findByCollectionId(String id) {
        return this.collectionDocumentRepository.findCollectionDocumentsByCollectionId(id);
    }
}
