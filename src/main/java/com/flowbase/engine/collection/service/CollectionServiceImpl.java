package com.flowbase.engine.collection.service;

import com.flowbase.engine.collection.domain.Collection;
import com.flowbase.engine.collection.domain.CollectionField;
import com.flowbase.engine.collection.domain.CollectionRepository;
import com.flowbase.engine.collection.domain.FieldType;
import com.flowbase.engine.collection.dto.CollectionFieldRequest;
import com.flowbase.engine.collection.dto.CollectionFieldResponse;
import com.flowbase.engine.collection.dto.CollectionResponse;
import com.flowbase.engine.collection.dto.CreateCollectionRequest;
import com.flowbase.engine.collection.exception.CollectionNotFoundException;
import com.flowbase.engine.collection.exception.ValidationException;
import com.flowbase.engine.common.service.IdGenerator;
import com.flowbase.engine.config.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
class CollectionServiceImpl implements CollectionService {
    private final CollectionRepository collectionRepository;
    private final IdGenerator idGenerator;
    private final JdbcTemplate jdbcTemplate;
    
    @Override
    public CollectionResponse createCollection(CreateCollectionRequest request) {
        String tenantId = TenantContext.get();
        if (tenantId == null) throw new RuntimeException("Tenant context is empty!");
        Optional<Collection> collectionExists = this.collectionRepository.findByTenantIdAndName(tenantId, request.name());
        if (collectionExists.isPresent())
            throw new ValidationException("Collection name " + request.name() + " already exists in this workspace!");
        Collection collection = new Collection().id(this.idGenerator.generate())
                                                .tenantId(tenantId)
                                                .name(request.name())
                                                .readRule(request.readRule())
                                                .writeRule(request.writeRule())
                                                .cacheTtlSeconds(request.cacheTtlSeconds())
                                                .lockTtlSeconds(request.lockTtlSeconds());
        List<CollectionField> fields = request.
                fields().
                stream().
                map(r -> new CollectionField(
                        this.idGenerator.generate(), r.name(), r.type(), r.required(), r.indexed(), r.searchable(), collection)
                ).toList();
        collection.fields(fields);
        this.collectionRepository.save(collection);
        for (CollectionField field : collection.fields()) {
            if (field.indexed()) {
                String cleanColId = collection.id().replace("-", "_");
                String indexName = "idx_doc_col_" + cleanColId + "_" + field.name();
                String indexSql = String.format(
                        "CREATE INDEX IF NOT EXISTS %s ON COLLECTION_DOCUMENTS (COLLECTION_ID, (DATA->>'%s'))",
                        indexName,
                        field.name()
                );
                this.jdbcTemplate.execute(indexSql);
            }
        }
        return CollectionResponse.from(collection);
    }
    
    @Override
    public List<CollectionResponse> listCollections(String tenantId) {
        List<Collection> tenantCollections = this.collectionRepository.findByTenantId(tenantId);
        return tenantCollections.stream().map(CollectionResponse::from).toList();
    }
    
    @Override
    public CollectionResponse getCollection(String id) {
        Optional<Collection> collectionExists = this.collectionRepository.findById(id);
        if (collectionExists.isEmpty()) throw new CollectionNotFoundException(id);
        Collection collection = collectionExists.get();
        if (!collection.tenantId().equals(TenantContext.get())) throw new CollectionNotFoundException(id);
        return CollectionResponse.from(collection);
    }
    
    @Override
    public void deleteCollection(String id) {
        CollectionResponse collectionResponse = this.getCollection(id);
        this.collectionRepository.delete(collectionResponse.id());
    }
}
