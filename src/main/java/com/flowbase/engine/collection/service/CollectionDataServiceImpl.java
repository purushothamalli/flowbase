package com.flowbase.engine.collection.service;

import com.flowbase.engine.collection.domain.Collection;
import com.flowbase.engine.collection.domain.CollectionDocument;
import com.flowbase.engine.collection.domain.CollectionDocumentRepository;
import com.flowbase.engine.collection.domain.CollectionRepository;
import com.flowbase.engine.collection.exception.CollectionNotFoundException;
import com.flowbase.engine.collection.exception.DocumentNotFoundException;
import com.flowbase.engine.collection.query.QueryContext;
import com.flowbase.engine.common.service.IdGenerator;
import com.flowbase.engine.config.TenantContext;
import com.flowbase.engine.realtime.event.DocumentChangeEvent;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;
    
    private final DocumentValidator documentValidator;
    private final DocumentAclService documentAclService;
    private final CacheLockManager cacheLockManager;
    private final DocumentQueryExecutor documentQueryExecutor;

    @Override
    @Transactional
    public CollectionDocument insertDocument(String collectionId, Map<String, Object> payload) {
        Optional<Collection> collectionExists = this.collectionRepository.findById(collectionId);
        if (collectionExists.isEmpty()) throw new CollectionNotFoundException(collectionId);
        Collection collection = collectionExists.get();
        
        // 1. ACL write check
        this.documentAclService.evaluateWriteAcl(collection, payload);
        
        // 2. Validate payload fields
        this.documentValidator.validate(collection, payload);
        
        CollectionDocument collectionDocument = new CollectionDocument(
            this.idGenerator.generate(), 
            collectionId, 
            payload, 
            Instant.now(), 
            Instant.now()
        );
        
        // 3. Evict caches
        this.cacheLockManager.evictCollectionCache(collectionId);
        
        CollectionDocument saved = this.collectionDocumentRepository.save(collectionDocument);
        this.eventPublisher.publishEvent(new DocumentChangeEvent(this, "insert", collectionId, saved));
        return saved;
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
    @Transactional(readOnly = true)
    public List<CollectionDocument> findDocumentsByCollection(String collectionId, QueryContext queryContext) {
        Optional<Collection> collectionExists = this.collectionRepository.findById(collectionId);
        if (collectionExists.isEmpty() || !collectionExists.get().tenantId().equals(TenantContext.get()))
            return List.of(); 
        Collection collection = collectionExists.get();
        
        String cacheKey = this.cacheLockManager.generateCacheKey(
            collectionId, 
            "list", 
            queryContext.rootNode(), 
            queryContext.sortBy(), 
            queryContext.limit()
        );
        boolean bypassCache = collection.readRule() != null && collection.readRule().contains("#auth");
        
        if (!bypassCache) {
            List<CollectionDocument> cached = this.cacheLockManager.getCachedList(cacheKey);
            if (cached != null) return this.documentAclService.filterReadAcl(collection, cached);
        }
        
        List<CollectionDocument> results = this.cacheLockManager.getDocumentsWithStampedeProtection(
            cacheKey, 
            bypassCache, 
            collection, 
            () -> this.documentQueryExecutor.queryDocuments(collection, queryContext)
        );
        return this.documentAclService.filterReadAcl(collection, results);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<CollectionDocument> searchDocuments(String collectionId, String searchQuery, int limit, int offset) {
        Optional<Collection> collectionExists = this.collectionRepository.findById(collectionId);
        if (collectionExists.isEmpty() || !collectionExists.get().tenantId().equals(TenantContext.get()))
            return List.of(); 
        Collection collection = collectionExists.get();
        
        String cacheKey = this.cacheLockManager.generateCacheKey(collectionId, "search", searchQuery, limit, offset);
        boolean bypassCache = collection.readRule() != null && collection.readRule().contains("#auth");
        
        if (!bypassCache) {
            List<CollectionDocument> cached = this.cacheLockManager.getCachedList(cacheKey);
            if (cached != null) return this.documentAclService.filterReadAcl(collection, cached);
        }
        
        List<CollectionDocument> results = this.cacheLockManager.getDocumentsWithStampedeProtection(
            cacheKey, 
            bypassCache, 
            collection, 
            () -> this.documentQueryExecutor.searchDocuments(collectionId, searchQuery, limit, offset)
        );
        return this.documentAclService.filterReadAcl(collection, results);
    }
    
    @Override
    @Transactional
    public CollectionDocument updateDocument(String collectionId, String documentId, Map<String, Object> payload) {
        CollectionDocument document = this.getDocument(collectionId, documentId);
        Optional<Collection> collectionExists = this.collectionRepository.findById(document.collectionId());
        if (collectionExists.isEmpty()) throw new CollectionNotFoundException(document.collectionId());
        Collection collection = collectionExists.get(); 
        
        Map<String, Object> data = document.data();
        Map<String, Object> merged = new HashMap<>(data); 
        merged.putAll(payload);
        
        // 1. ACL write check
        this.documentAclService.evaluateWriteAcl(collection, merged); 
        
        // 2. Validate payload fields
        this.documentValidator.validate(collection, merged);
        
        document.data(merged); 
        document.updatedAt(Instant.now());
        
        // 3. Evict caches
        this.cacheLockManager.evictCollectionCache(collectionId);
        
        CollectionDocument updated = this.collectionDocumentRepository.save(document);
        this.eventPublisher.publishEvent(new DocumentChangeEvent(this, "update", collectionId, updated));
        return updated;
    }
    
    @Override
    @Transactional
    public void deleteDocument(String collectionId, String documentId) {
        CollectionDocument document = this.getDocument(collectionId, documentId);
        Optional<Collection> collectionExists = this.collectionRepository.findById(document.collectionId());
        if (collectionExists.isEmpty()) throw new CollectionNotFoundException(document.collectionId());
        
        // 1. ACL write check on deletion payload state
        this.documentAclService.evaluateWriteAcl(collectionExists.get(), document.data()); 
        
        this.collectionDocumentRepository.delete(document);
        
        // 2. Evict caches
        this.cacheLockManager.evictCollectionCache(collectionId);
        
        this.eventPublisher.publishEvent(new DocumentChangeEvent(this, "delete", collectionId, document));
    }
}
