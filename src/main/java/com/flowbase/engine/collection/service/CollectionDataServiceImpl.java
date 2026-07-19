package com.flowbase.engine.collection.service;

import com.flowbase.engine.collection.domain.Collection;
import com.flowbase.engine.collection.domain.CollectionDocument;
import com.flowbase.engine.collection.domain.CollectionDocumentRepository;
import com.flowbase.engine.collection.domain.CollectionField;
import com.flowbase.engine.collection.domain.CollectionRepository;
import com.flowbase.engine.collection.exception.CollectionNotFoundException;
import com.flowbase.engine.collection.exception.DocumentNotFoundException;
import com.flowbase.engine.collection.query.CompiledQuery;
import com.flowbase.engine.collection.query.QueryCompiler;
import com.flowbase.engine.collection.query.QueryContext;
import com.flowbase.engine.collection.validation.ValidationRule;
import com.flowbase.engine.common.service.IdGenerator;
import com.flowbase.engine.config.TenantContext;
import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

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
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final QueryCompiler queryCompiler;
    private final ObjectMapper objectMapper;
    
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
    public List<CollectionDocument> findDocumentsByCollection(String collectionId, QueryContext queryContext) {
        Optional<Collection> collectionExists = this.collectionRepository.findById(collectionId);
        if (collectionExists.isEmpty() || !collectionExists.get().tenantId().equals(TenantContext.get()))
            return List.of();
        Collection collection = collectionExists.get();
        CompiledQuery compiled = this.queryCompiler.compile(collection, queryContext);
        StringBuilder sql = new StringBuilder(
                "SELECT ID, COLLECTION_ID, DATA, CREATED_AT, UPDATED_AT FROM COLLECTION_DOCUMENTS WHERE " + compiled.sql());
        Map<String, Object> paramMap = new HashMap<>(compiled.params());
        if (!queryContext.sortBy().isEmpty()) {
            String sortQuery = queryContext.sortBy().trim();
            String sortOrder = "ASC";
            if (sortQuery.startsWith("-")) {sortOrder = "DESC"; sortQuery = sortQuery.substring(1);}
            if (sortQuery.equals("createdAt") || sortQuery.equals("updatedAt"))
                sql.append(" ORDER BY ")
                   .append(sortQuery.equals("createdAt") ? "created_at " : "updated_at ")
                   .append(sortOrder);
            else {
                String finalSortQuery = sortQuery;
                boolean match =
                        collection.fields()
                                  .stream()
                                  .anyMatch(collectionField -> collectionField.name().equals(finalSortQuery));
                if (match) sql.append(" ORDER BY DATA ->> '").append(sortQuery).append("' ").append(sortOrder);
            }
        }
        if (queryContext.limit() > 0) {
            sql.append(" LIMIT :limit_const");
            paramMap.put("limit_const", queryContext.limit());
        }
        if (queryContext.offset() > 0) {
            sql.append(" OFFSET :offset_const");
            paramMap.put("offset()_const", queryContext.offset());
        }
        return this.jdbcTemplate.query(sql.toString(), paramMap, (rs, rowNum) -> {
            try {
                String id = rs.getString("id");
                String collectionId1 = rs.getString("collection_id");
                String rawData = rs.getString("data");
                Map<String, Object> data = this.objectMapper.readValue(rawData, new TypeReference<Map<String, Object>>() {});
                Instant createdAt = rs.getTimestamp("created_at").toInstant();
                Instant updatedAt = rs.getTimestamp("updated_at").toInstant();
                return new CollectionDocument(id, collectionId1, data, createdAt, updatedAt);
            } catch (Exception e) {
                throw new RuntimeException("Failed to map document row: ", e);
            }
        });
//        return this.collectionDocumentRepository.findByCollectionId(collectionId);
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
