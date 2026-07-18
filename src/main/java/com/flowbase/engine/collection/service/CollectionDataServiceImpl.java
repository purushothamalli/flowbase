package com.flowbase.engine.collection.service;

import com.flowbase.engine.collection.domain.Collection;
import com.flowbase.engine.collection.domain.CollectionDocument;
import com.flowbase.engine.collection.domain.CollectionDocumentRepository;
import com.flowbase.engine.collection.domain.CollectionField;
import com.flowbase.engine.collection.domain.CollectionRepository;
import com.flowbase.engine.collection.exception.CollectionNotFoundException;
import com.flowbase.engine.collection.exception.DocumentNotFoundException;
import com.flowbase.engine.collection.validation.ValidationRule;
import com.flowbase.engine.common.service.IdGenerator;
import com.flowbase.engine.config.TenantContext;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Service
@AllArgsConstructor
class CollectionDataServiceImpl implements CollectionDataService {
    private final CollectionRepository collectionRepository;
    private final CollectionDocumentRepository collectionDocumentRepository;
    private final IdGenerator idGenerator;
    private final List<ValidationRule> validationRules;
    
    @Override
    @Transactional
    public CollectionDocument insertDocument(String collectionId, Map<String, Object> payload) {
        Optional<Collection> collectionExists = this.collectionRepository.findById(collectionId);
        if (collectionExists.isEmpty()) throw new CollectionNotFoundException(collectionId);
        Collection collection = collectionExists.get();
        for (CollectionField field : collection.fields()) {
            Object value = payload.get(field.name());
            for (ValidationRule rule : this.validationRules) {
                rule.validate(field, value);
            }
        }
        CollectionDocument collectionDocument = new CollectionDocument(
                this.idGenerator.generate(),
                collectionId,
                payload,
                Instant.now(),
                Instant.now()
        );
        return this.collectionDocumentRepository.save(collectionDocument);
    }
    
    @Override
    public CollectionDocument getDocument(String collectionId, String documentId) {
        Optional<Collection> collectionExists = this.collectionRepository.findById(collectionId);
        if (collectionExists.isEmpty() || !collectionExists.get().tenantId().equals(TenantContext.get()))
            throw new CollectionNotFoundException(collectionId);
        Optional<CollectionDocument> documentExists = this.collectionDocumentRepository.findById(documentId);
        if (documentExists.isEmpty() || !documentExists.get().collectionId().equals(collectionExists.get().id()))
            throw new DocumentNotFoundException(documentId);
        return documentExists.get();
    }
    
    @Override
    public List<CollectionDocument> findDocumentsByCollection(String collectionId) {
        Optional<Collection> collectionExists = this.collectionRepository.findById(collectionId);
        if (collectionExists.isEmpty() || !collectionExists.get().tenantId().equals(TenantContext.get()))
            return List.of();
        return this.collectionDocumentRepository.findByCollectionId(collectionId);
    }
    
    @Override
    @Transactional
    public CollectionDocument updateDocument(String collectionId, String documentId, Map<String, Object> payload) {
        CollectionDocument document = this.getDocument(collectionId, documentId);
        Optional<Collection> collectionExists = this.collectionRepository.findById(document.collectionId());
        if (collectionExists.isEmpty()) throw new CollectionNotFoundException(document.collectionId());
        Map<String, Object> data = document.data();
        Map<String, Object> merged = new HashMap<>(data);
        merged.putAll(payload);
        for (CollectionField field : collectionExists.get().fields()) {
            Object value = merged.get(field.name());
            for (ValidationRule rule : this.validationRules) {
                rule.validate(field, value);
            }
        }
        document.data(merged);
        document.updatedAt(Instant.now());
        return this.collectionDocumentRepository.save(document);
    }
    
    @Override
    @Transactional
    public void deleteDocument(String collectionId, String documentId) {
        CollectionDocument document = this.getDocument(collectionId, documentId);
        this.collectionDocumentRepository.delete(document);
    }
}
