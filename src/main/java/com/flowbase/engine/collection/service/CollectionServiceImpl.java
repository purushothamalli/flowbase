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
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
class CollectionServiceImpl implements CollectionService {
    private final CollectionRepository collectionRepository;
    private final IdGenerator idGenerator;
    
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
                                                .writeRule(request.writeRule());
        List<CollectionField> fields = new ArrayList<>();
        for (CollectionFieldRequest fieldRequest : request.fields()) {
            CollectionField field = new CollectionField(this.idGenerator.generate(), fieldRequest.name(), fieldRequest.type(), fieldRequest.required(), collection);
            fields.add(field);
        } collection.fields(fields);
        return this.createCollectionResponse(this.collectionRepository.save(collection));
    }
    
    private CollectionFieldResponse createCollectionFieldResponse(CollectionField collectionField) {
        return new CollectionFieldResponse(collectionField.id(), collectionField.name(), collectionField.type(), collectionField.required());
    }
    
    private CollectionResponse createCollectionResponse(Collection collection) {
        List<CollectionFieldResponse> collectionFieldResponseList = collection.fields()
                                                                              .stream()
                                                                              .map(this::createCollectionFieldResponse)
                                                                              .toList();
        return new CollectionResponse(collection.id(), collection.name(), collection.readRule(),
                collection.writeRule(), collectionFieldResponseList);
    }
    
    @Override
    public List<CollectionResponse> listCollections(String tenantId) {
        List<Collection> tenantCollections = this.collectionRepository.findByTenantId(tenantId);
        return tenantCollections.stream().map(this::createCollectionResponse).toList();
    }
    
    @Override
    public CollectionResponse getCollection(String id) {
        Optional<Collection> collectionExists = this.collectionRepository.findById(id);
        if (collectionExists.isEmpty()) throw new CollectionNotFoundException(id);
        Collection collection = collectionExists.get();
        if (!collection.tenantId().equals(TenantContext.get())) throw new CollectionNotFoundException(id);
        return this.createCollectionResponse(collection);
    }
    
    @Override
    public void deleteCollection(String id) {
        CollectionResponse collectionResponse = this.getCollection(id);
        this.collectionRepository.delete(collectionResponse.id());
    }
}
