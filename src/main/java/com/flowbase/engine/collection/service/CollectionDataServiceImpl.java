package com.flowbase.engine.collection.service;

import com.flowbase.engine.auth.dto.AuthenticatedUser;
import com.flowbase.engine.collection.domain.Collection;
import com.flowbase.engine.collection.domain.CollectionDocument;
import com.flowbase.engine.collection.domain.CollectionDocumentRepository;
import com.flowbase.engine.collection.domain.CollectionField;
import com.flowbase.engine.collection.domain.CollectionRepository;
import com.flowbase.engine.collection.exception.AclDeniedException;
import com.flowbase.engine.collection.exception.CollectionNotFoundException;
import com.flowbase.engine.collection.exception.DocumentNotFoundException;
import com.flowbase.engine.collection.query.CompiledQuery;
import com.flowbase.engine.collection.query.QueryCompiler;
import com.flowbase.engine.collection.query.QueryContext;
import com.flowbase.engine.collection.validation.ValidationRule;
import com.flowbase.engine.common.service.IdGenerator;
import com.flowbase.engine.config.TenantContext;
import com.flowbase.engine.config.UserContext;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;


@Service
@AllArgsConstructor
class CollectionDataServiceImpl implements CollectionDataService {
    private final CollectionRepository collectionRepository;
    private final CollectionDocumentRepository collectionDocumentRepository;
    private final IdGenerator idGenerator;
    private final List<ValidationRule> validationRules;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final QueryCompiler queryCompiler;
    private final ObjectMapper objectMapper;
    private final CacheService cacheService;
    private final AclEvaluatorService aclEvaluatorService;
    
    @Override
    @Transactional
    public CollectionDocument insertDocument(String collectionId, Map<String, Object> payload) {
        Optional<Collection> collectionExists = this.collectionRepository.findById(collectionId);
        if (collectionExists.isEmpty()) throw new CollectionNotFoundException(collectionId);
        Collection collection = collectionExists.get(); this.aclEvaluation(collection, payload);
        for (CollectionField field : collection.fields()) {
            Object value = payload.get(field.name()); for (ValidationRule rule : this.validationRules) {
                rule.validate(field, value);
            }
        }
        CollectionDocument collectionDocument = new CollectionDocument(this.idGenerator.generate(), collectionId, payload, Instant.now(), Instant.now());
        this.cacheService.evictNamespace("flowbase:cache:" + collectionId + ":");
        return this.collectionDocumentRepository.save(collectionDocument);
    }
    
    @Override
    public CollectionDocument getDocument(String collectionId, String documentId) {
        Optional<Collection> collectionExists = this.collectionRepository.findById(collectionId);
        if (collectionExists.isEmpty() || !collectionExists.get().tenantId().equals(TenantContext.get()))
            throw new CollectionNotFoundException(collectionId);
        Optional<CollectionDocument> documentExists = this.collectionDocumentRepository.findById(documentId);
        if (documentExists.isEmpty() || !documentExists.get().collectionId().equals(collectionExists.get().id()))
            throw new DocumentNotFoundException(documentId); return documentExists.get();
    }
    
    @Override
    public List<CollectionDocument> findDocumentsByCollection(String collectionId, QueryContext queryContext) {
        Optional<Collection> collectionExists = this.collectionRepository.findById(collectionId);
        if (collectionExists.isEmpty() || !collectionExists.get().tenantId().equals(TenantContext.get()))
            return List.of(); Collection collection = collectionExists.get();
        String cacheKey = this.keyGenerator(collectionId, "list", queryContext.filters(), queryContext.sortBy(), queryContext.limit());
        boolean bypassCache = collection.readRule() != null && collection.readRule().contains("#auth");
        if (!bypassCache) {
            List<CollectionDocument> cached = this.cacheService.getList(cacheKey, CollectionDocument.class);
            if (cached != null) return this.filterDocuments(collection, cached);
        } CompiledQuery compiled = this.queryCompiler.compile(collection, queryContext);
        StringBuilder sql = new StringBuilder("SELECT ID, COLLECTION_ID, DATA, CREATED_AT, UPDATED_AT FROM COLLECTION_DOCUMENTS WHERE " + compiled.sql());
        Map<String, Object> paramMap = new HashMap<>(compiled.params()); if (!queryContext.sortBy().isEmpty()) {
            String sortQuery = queryContext.sortBy().trim(); String sortOrder = "ASC";
            if (sortQuery.startsWith("-")) {sortOrder = "DESC"; sortQuery = sortQuery.substring(1);}
            if (sortQuery.equals("createdAt") || sortQuery.equals("updatedAt")) sql.append(" ORDER BY ")
                                                                                   .append(sortQuery.equals("createdAt") ? "created_at " : "updated_at ")
                                                                                   .append(sortOrder);
            else {
                String finalSortQuery = sortQuery; boolean match = collection.fields()
                                                                             .stream()
                                                                             .anyMatch(collectionField -> collectionField.name()
                                                                                                                         .equals(finalSortQuery));
                if (match) sql.append(" ORDER BY DATA ->> '").append(sortQuery).append("' ").append(sortOrder);
            }
        } if (queryContext.limit() > 0) {
            sql.append(" LIMIT :limit_const"); paramMap.put("limit_const", queryContext.limit());
        } if (queryContext.offset() > 0) {
            sql.append(" OFFSET :offset_const"); paramMap.put("offset_const", queryContext.offset());
        }
        return this.filterDocuments(collection, this.getDocumentsWithStampedeProtection(cacheKey, bypassCache, () -> this.jdbcTemplate.query(sql.toString(), paramMap, (rs, rowNum) -> {
            try {
                return getCollectionDocument(rs);
            } catch (Exception e) {
                throw new RuntimeException("Failed to map document row: ", e);
            }
        })));
    }
    
    @Override
    public List<CollectionDocument> searchDocuments(String collectionId, String searchQuery, int limit, int offset) {
        Optional<Collection> collectionExists = this.collectionRepository.findById(collectionId);
        if (collectionExists.isEmpty() || !collectionExists.get().tenantId().equals(TenantContext.get()))
            return List.of(); Collection collection = collectionExists.get();
        String cacheKey = this.keyGenerator(collectionId, "search", searchQuery, limit, offset);
        boolean bypassCache = collection.readRule() != null && collection.readRule().contains("#auth");
        if (!bypassCache) {
            List<CollectionDocument> cached = this.cacheService.getList(cacheKey, CollectionDocument.class);
            if (cached != null) return this.filterDocuments(collection, cached);
        } int safeLimit = limit <= 0 ? 20 : Math.clamp(limit, 1, 100); int safeOffset = Math.max(0, offset);
        String sql = "SELECT id, collection_id, data, created_at, updated_at, " + " ts_rank(tsv_document, query) as rank " + "FROM collection_documents, websearch_to_tsquery('english', :search_query) query " + "WHERE collection_id = :collectionId_const " + "  AND tsv_document @@ query " + "ORDER BY rank DESC " + "LIMIT :limit_const OFFSET :offset_const";
        Map<String, Object> paramMap = new HashMap<>(); paramMap.put("collectionId_const", collectionId);
        paramMap.put("search_query", searchQuery); paramMap.put("limit_const", safeLimit);
        paramMap.put("offset_const", safeOffset);
        return this.filterDocuments(collection, this.getDocumentsWithStampedeProtection(cacheKey, bypassCache, () -> this.jdbcTemplate.query(sql, paramMap, (rs, rowNum) -> {
            try {
                return getCollectionDocument(rs);
            } catch (Exception e) {
                throw new RuntimeException("Failed to map document row: ", e);
            }
        })));
    }
    
    @Override
    @Transactional
    public CollectionDocument updateDocument(String collectionId, String documentId, Map<String, Object> payload) {
        CollectionDocument document = this.getDocument(collectionId, documentId);
        Optional<Collection> collectionExists = this.collectionRepository.findById(document.collectionId());
        if (collectionExists.isEmpty()) throw new CollectionNotFoundException(document.collectionId());
        Collection collection = collectionExists.get(); Map<String, Object> data = document.data();
        Map<String, Object> merged = new HashMap<>(data); merged.putAll(payload);
        this.aclEvaluation(collection, merged); for (CollectionField field : collectionExists.get().fields()) {
            Object value = merged.get(field.name()); for (ValidationRule rule : this.validationRules) {
                rule.validate(field, value);
            }
        } document.data(merged); document.updatedAt(Instant.now());
        this.cacheService.evictNamespace("flowbase:cache:" + collectionId + ":");
        return this.collectionDocumentRepository.save(document);
    }
    
    @Override
    @Transactional
    public void deleteDocument(String collectionId, String documentId) {
        CollectionDocument document = this.getDocument(collectionId, documentId);
        Optional<Collection> collectionExists = this.collectionRepository.findById(document.collectionId());
        if (collectionExists.isEmpty()) throw new CollectionNotFoundException(document.collectionId());
        this.aclEvaluation(collectionExists.get(), document.data()); this.collectionDocumentRepository.delete(document);
        this.cacheService.evictNamespace("flowbase:cache:" + collectionId + ":");
    }
    
    @NonNull
    private CollectionDocument getCollectionDocument(ResultSet rs) throws SQLException {
        String id = rs.getString("id"); String collId = rs.getString("collection_id");
        String rawData = rs.getString("data");
        Map<String, Object> data = this.objectMapper.readValue(rawData, new TypeReference<Map<String, Object>>() {});
        Instant createdAt = rs.getTimestamp("created_at").toInstant();
        Instant updatedAt = rs.getTimestamp("updated_at").toInstant();
        return new CollectionDocument(id, collId, data, createdAt, updatedAt);
    }
    
    private String keyGenerator(String collectionId, Object... inputs) {
        try {
            StringBuilder sb = new StringBuilder(); for (Object input : inputs) {
                sb.append(input != null ? input.toString() : "null").append("|");
            } MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(); for (byte b : hash) {
                String hexString = Integer.toHexString(0xff & b); if (hexString.length() == 1) hex.append('0');
                hex.append(hexString);
            } return "flowbase:cache:" + collectionId + ":" + hex;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate CacheKey :", e);
        }
    }
    
    private List<CollectionDocument> getDocumentsWithStampedeProtection(String cacheKey, boolean bypassCache, Supplier<List<CollectionDocument>> dbQuerySupplier) {
        if (bypassCache) return dbQuerySupplier.get();
        List<CollectionDocument> cached = this.cacheService.getList(cacheKey, CollectionDocument.class);
        if (cached != null) return cached; String lockKey = cacheKey + ":lock";
        String lockValue = UUID.randomUUID().toString(); if (this.cacheService.acquireLock(lockKey, lockValue, 5)) {
            try {
                cached = this.cacheService.getList(cacheKey, CollectionDocument.class);
                if (cached != null) return cached; List<CollectionDocument> dbResult = dbQuerySupplier.get();
                this.cacheService.put(cacheKey, dbResult, 3600); return dbResult;
            } finally {
                this.cacheService.releaseLock(lockKey, lockValue);
            }
        } else {
            int retries = 10; while (retries > 0) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); break;
                } cached = this.cacheService.getList(cacheKey, CollectionDocument.class);
                if (cached != null) return cached; retries--;
            } return dbQuerySupplier.get();
        }
    }
    
    private void aclEvaluation(Collection collection, Map<String, Object> payload) {
        AuthenticatedUser user = UserContext.get(); String userId = user != null ? user.id() : null;
        String userRole = user != null ? user.role().name() : null;
        if (!this.aclEvaluatorService.evaluate(collection.writeRule(), payload, userId, userRole))
            throw new AclDeniedException("Write access denied by ACL policy.");
    }
    
    private List<CollectionDocument> filterDocuments(Collection collection, List<CollectionDocument> documents) {
        if (collection.readRule() == null || collection.readRule().isEmpty() || documents.isEmpty()) return documents;
        AuthenticatedUser currentUser = UserContext.get();
        String userId = currentUser != null ? currentUser.id() : null;
        String userRole = currentUser != null && currentUser.role() != null ? currentUser.role().name() : null;
        return documents.stream()
                        .filter(doc -> this.aclEvaluatorService.evaluate(collection.readRule(), doc.data(), userId, userRole))
                        .toList();
    }
}

