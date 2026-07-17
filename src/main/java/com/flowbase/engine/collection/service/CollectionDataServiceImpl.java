package com.flowbase.engine.collection.service;

import com.flowbase.engine.collection.domain.Collection;
import com.flowbase.engine.collection.domain.CollectionDocument;
import com.flowbase.engine.collection.domain.CollectionDocumentRepository;
import com.flowbase.engine.collection.domain.CollectionField;
import com.flowbase.engine.collection.domain.CollectionRepository;
import com.flowbase.engine.collection.exception.CollectionNotFoundException;
import com.flowbase.engine.collection.exception.ValidationException;
import com.flowbase.engine.collection.validation.ValidationRule;
import com.flowbase.engine.common.service.IdGenerator;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
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
    public CollectionDocument insertDocument(String collectionId, Map<String, Object> payload) {
        Optional<Collection> collectionExists = this.collectionRepository.findById(collectionId);
        if (collectionExists.isEmpty()) throw new CollectionNotFoundException("Collection with given Id not found!");
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
}
